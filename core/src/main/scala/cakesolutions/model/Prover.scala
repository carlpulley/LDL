package cakesolutions.model

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import cakesolutions.model.QueryModel._
import cakesolutions.model.provers.{CVC4, Z3}
import cakesolutions.syntax.QueryLanguage
import cakesolutions.syntax.QueryLanguage.Query

import scala.util.Failure

trait Prover extends StandardEvaluation with Actor with ActorLogging {

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

  private[this] var proofContext: QueryLanguage.Observation = Set.empty

  def receive = unstable(query)

  private def stable(result: StableValue): Receive = LoggingReceive {
    case _: Assertion =>
      // Nothing to do

    case _: ObservableEvent =>
      sender() ! result
  }

  private def unstable(query: Query): Receive = LoggingReceive {
    case Assertion(observable) =>
      proofContext = proofContext ++ observable

    case event: ObservableEvent =>
      processEvent(event, query)
      proofContext = Set.empty
  }

  private def processEvent(event: ObservableEvent, query: Query): Unit = {
      evaluateQuery(query, event, proofContext) match {
        case UnstableValue(Some(nextQuery)) =>
          // We interact with the prover concurrently to determine validity of, satisfiability of and simplify `nextQuery`
          prover.valid(nextQuery).foreach { validQuery =>
            if (validQuery) {
              val result = StableValue(result = true)
              context.become(stable(result))
              // `nextQuery` is valid - so any LDL unwinding of this formula will allow it to become true
              context.system.log.debug(s"Processed event - result: $result")
              sender() ! result
            } else {
              prover.satisfiable(nextQuery).foreach { satisfiableQuery =>
                if (satisfiableQuery) {
                  prover.simplify(nextQuery).foreach { simplifiedQuery =>
                    // We need to unwind the LDL formula further in order to determine its validity
                    context.become(unstable(simplifiedQuery))
                    // We pass on next query here to "facilitate" decision making on repeated query matching
                    context.system.log.debug("Processed event - result: Unstable()")
                    sender() ! UnstableValue()
                  }
                } else {
                  val result = StableValue(result = false)
                  context.become(stable(result))
                  // `nextQuery` is unsatisfiable - so no LDL unwinding of this formula will allow it to become true
                  context.system.log.debug(s"Processed event - result: $result")
                  sender() ! result
                }
              }
            }
          }

        case UnstableValue(None) =>
          Failure(new Exception("Unexpected message observered - prover should *only* receive qualified unstable values!"))

        case value: StableValue =>
          context.become(stable(value))
          context.system.log.debug(s"Processed event - result: $value")
          sender() ! value
      }
  }

}
