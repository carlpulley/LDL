package cakesolutions.model

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.pipe
import cakesolutions.model.QueryModel._
import cakesolutions.model.provers.{CVC4, Z3}
import cakesolutions.syntax.QueryLanguage.Query

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait Prover extends StandardEvaluation with Actor with ActorLogging {

  import context.dispatcher

  def query: Query

  private[this] val config = context.system.settings.config
  private[this] val prover = config.getString("prover.default") match {
    case "cvc4" | "CVC4" =>
      context.system.log.info("Loading CVC4 prover")
      new CVC4(config)

    case "z3" | "Z3" =>
      context.system.log.info("Loading Z3 prover")
      new Z3(config)

    case value: String =>
      sys.error(s"$value is an unknown prover!")
  }

  def receive = unstable(query)

  private def stable(result: StableValue): Receive = LoggingReceive {
    case _: Event =>
      sender() ! result
  }

  private def unstable(query: Query): Receive = LoggingReceive {
    case event: Event =>
      Future.fromTry(processEvent(event, query)) pipeTo sender()
  }

  private def processEvent(event: Event, query: Query): Try[Notification] = event match {
    case Assertion(observable) =>
      evaluateQueryAssert(query, observable) match {
        case UnstableValue(Some(currentQuery)) =>
          prover.satisfiable(currentQuery).flatMap { satisfiableQuery =>
            if (satisfiableQuery) {
              prover.simplify(currentQuery).map { simplifiedQuery =>
                // We need to unwind the LDL formula further in order to determine its validity
                context.become(unstable(simplifiedQuery))
                // We pass on next query here to "facilitate" decision making on repeated query matching
                UnstableValue()
              }
            } else {
              val result = StableValue(result = false)
              context.become(stable(result))
              // `updatedQuery` is unsatisfiable - so no LDL unwinding of this formula will allow it to become true
              Success(result)
            }
          }

        case UnstableValue(None) =>
          Failure(new Exception("Unexpected message asserted - prover should *only* receive qualified unstable values!"))

        case value: StableValue =>
          context.become(stable(value))
          Success(value)
      }

    case observable: ObservableEvent =>
      evaluateQueryNext(query, observable) match {
        case UnstableValue(Some(nextQuery)) =>
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
                    UnstableValue()
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

        case UnstableValue(None) =>
          Failure(new Exception("Unexpected message observered - prover should *only* receive qualified unstable values!"))

        case value: StableValue =>
          context.become(stable(value))
          Success(value)
      }
  }

}
