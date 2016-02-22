package cakesolutions.monitor
package model

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.pipe
import cakesolutions.model.QueryModel._
import cakesolutions.model.StandardEvaluation
import cakesolutions.model.provers.{CVC4, Z3}
import cakesolutions.syntax.QueryLanguage.Query

import scala.concurrent.Future
import scala.util.{Success, Try}

private[monitor] trait Prover extends StandardEvaluation {
  this: Actor with ActorLogging =>

  import context.dispatcher

  def query: Query

  private[this] val config = context.system.settings.config
  private[this] val prover = config.getString("prover.default") match {
    case "cvc4" | "CVC4" =>
      new CVC4(config)

    case "z3" | "Z3" =>
      new Z3(config)
  }

  def receive = unstable(query)

  private def unstable(query: Query): Receive = LoggingReceive {
    case event: ObservableEvent =>
      Future.fromTry(processEvent(event, query)) pipeTo sender()
  }

  private def processEvent(event: ObservableEvent, query: Query): Try[Notification] =
    evaluateQuery(query)(event) match {
      case UnstableValue(nextQuery) =>
        // We interact with the prover concurrently to determine validity of, satisfiability of and simplify `nextQuery`
        prover.valid(nextQuery).flatMap { validQuery =>
          if (validQuery) {
            val result = StableValue(result = true)
            context.become(stable(result))
            // `nextQuery` is valid - so any LDL unwinding of this formula will allow it to become true
            Success(result)
          } else {
            prover.satisfiable(nextQuery).flatMap { satisfiableQuery =>
              if (satisfiableQuery) {
                prover.simplify(nextQuery).map { simplifiedQuery =>
                  // We need to unwind the LDL formula further in order to determine its validity
                  context.become(unstable(simplifiedQuery))
                  // We pass on next query here to "facilitate" decision making on repeated query matching
                  UnstableValue(nextQuery)
                }
              } else {
                val result = StableValue(result = false)
                context.become(stable(result))
                // `nextQuery` is unsatisfiable - so no LDL unwinding of this formula will allow it to become true
                Success(result)
              }
            }
          }
        }

      case value: StableValue =>
        context.become(stable(value))
        Success(value)
    }

  private def stable(result: StableValue): Receive = LoggingReceive {
    case _: ObservableEvent =>
      sender() ! result
  }

}
