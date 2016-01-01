package cakesolutions.monitor

import akka.actor._
import akka.event.LoggingReceive
import cakesolutions.model.QueryModel._
import cakesolutions.model.StandardEvaluation
import cakesolutions.model.provers.Interface
import cakesolutions.syntax.QueryLanguage.Query

import scala.async.Async._
import scala.concurrent.Future

/**
 * Internal API
 */
object Prover {

  private[monitor] case object Continue

  private[monitor] def props(query: Query)(implicit prover: Interface) = Props(new Prover(query))

}

/**
 * This actor performs a computationally intensive task, hence each actor instances should have a dedicated thread.
 *
 * @param query query that we are to monitor for
 */
class Prover(query: Query)(implicit prover: Interface) extends Actor with Stash with ActorLogging with StandardEvaluation {

  import Prover._
  import context.dispatcher

  private[this] var resultCache: Option[Notification] = None

  private def processEvent(event: ObservableEvent, query: Query): Future[Notification] = {
    // NOTE: as mutable state is updated, we need to ensure evaluation occurs on this thread (i.e. in a "synchronous" step)
    context.become(proving(query))

    log.debug(s"\n  $event")

    val stage =
      if (resultCache.isDefined) {
        resultCache.get
      } else {
        evaluateQuery(query)(event)
      }

    log.debug(s"\n  EVALUATE: $query\n  ~~> $stage")

    stage match {
      case UnstableValue(nextQuery) =>
        async {
          // We interact with the prover concurrently to determine validity of, satisfiability of and simplify `nextQuery`
          val validQuery = prover.valid(nextQuery)
          lazy val satisfiableQuery = prover.satisfiable(nextQuery)
          lazy val simplifiedQuery = prover.simplify(nextQuery)

          if (await(validQuery)) {
            // `nextQuery` is valid - so any LDL unwinding of this formula will allow it to become true
            StableValue(result = true)
          } else if (await(satisfiableQuery)) {
            // We need to unwind LDL formula further in order to determine its validity
            context.become(proof(await(simplifiedQuery)))
            // We pass on next query here to "facilitate" decision making on repeated query matching
            UnstableValue(nextQuery)
          } else {
            // `nextQuery` is unsatisfiable - so no LDL unwinding of this formula will allow it to become true
            StableValue(result = false)
          }
        }

      case value: StableValue =>
        resultCache = Some(value)

        Future(value)
    }
  }

  private def proving(query: Query): Receive = {
    case Cancel =>
      context.stop(self)

    case Continue =>
      context.become(proof(query))
      unstashAll()

    case _ =>
      stash()
  }

  private def proof(query: Query): Receive = LoggingReceive {
    case Cancel =>
      context.stop(self)

    case event @ Next(_, replyTo) =>
      processEvent(event, query).foreach { msg =>
        replyTo ! msg
        self ! Continue
      }

    case event @ Completed(_, replyTo) =>
      processEvent(event, query).foreach { msg =>
        replyTo ! msg
        self ! Continue
      }
  }

  def receive = proof(query)

}
