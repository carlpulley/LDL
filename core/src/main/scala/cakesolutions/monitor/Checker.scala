package cakesolutions.monitor

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern.pipe
import cakesolutions.model.QueryModel._
import cakesolutions.model.StandardEvaluation
import cakesolutions.model.provers.{CVC4, Z3}
import cakesolutions.syntax.QueryLanguage.Query

import scala.async.Async._
import scala.concurrent.Future

/**
 * Internal API
 */
object Checker {

  private[monitor] def props(query: Query) =
    // Each actor instance is single threaded - we do **not** allow sharing of SMT provers within a thread
    Props(new Checker(query)).withDispatcher("checker-dispatcher")

}

/**
 * TODO: document!
 *
 * @param query query that we are to monitor for
 */
class Checker(query: Query) extends Actor with ActorLogging with StandardEvaluation {

  import context.dispatcher

  private[this] val config = context.system.settings.config
  private[this] val prover = config.getString("prover.default") match {
    case "cvc4" | "CVC4" =>
      new CVC4(config)

    case "z3" | "Z3" =>
      new Z3(config)
  }

  private def processEvent(event: ObservableEvent, query: Query): Future[Notification] =
    evaluateQuery(query)(event) match {
      case UnstableValue(nextQuery) =>
        async {
          // We interact with the prover concurrently to determine validity of, satisfiability of and simplify `nextQuery`
          val validQuery = prover.valid(nextQuery)
          lazy val satisfiableQuery = prover.satisfiable(nextQuery)
          lazy val simplifiedQuery = prover.simplify(nextQuery)

          if (await(validQuery)) {
            context.become(ignore)
            // `nextQuery` is valid - so any LDL unwinding of this formula will allow it to become true
            StableValue(result = true)
          } else if (await(satisfiableQuery)) {
            // We need to unwind the LDL formula further in order to determine its validity
            context.become(prove(await(simplifiedQuery)))
            // We pass on next query here to "facilitate" decision making on repeated query matching
            UnstableValue(nextQuery)
          } else {
            context.become(ignore)
            // `nextQuery` is unsatisfiable - so no LDL unwinding of this formula will allow it to become true
            StableValue(result = false)
          }
        }

      case value: StableValue =>
        context.become(ignore)
        Future.successful(value)
    }

  private def ignore: Receive = LoggingReceive {
    case Cancel =>
      context.stop(self)

    case _: ObservableEvent =>
      // Ignore all observable events
  }

  private def prove(query: Query): Receive = LoggingReceive {
    case Cancel =>
      context.stop(self)

    case event: ObservableEvent =>
      processEvent(event, query) pipeTo sender()
  }

  def receive = prove(query)

}