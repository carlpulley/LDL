package cakesolutions.monitor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import cakesolutions.model.QueryModel._
import cakesolutions.model.StandardEvaluation
import cakesolutions.model.provers.CVC4
import cakesolutions.syntax.QueryLanguage.Query
import com.typesafe.config.ConfigFactory
import scala.async.Async._
import scala.concurrent.Future

/**
 * Internal API
 */
object CVC4Prover {

  private[monitor] case object Continue

  private[monitor] def props(query: Query) = Props(new CVC4Prover(query))

}

/**
 * This actor performs a computationally intensive task, hence each actor instance should have a dedicated thread.
 *
 * @param query query that we are to monitor for
 */
class CVC4Prover(query: Query) extends Actor with Stash with ActorLogging with StandardEvaluation {

  import CVC4Prover._
  import context.dispatcher

  private val config = context.system.settings.config
  private[this] val prover = new CVC4(ConfigFactory.load("prover.conf"))

  private[this] var resultCache: Option[Notification] = None

  private def proving(query: Query): Receive = {
    case Continue =>
      context.become(proof(query))
      unstashAll()

    case _ =>
      stash()
  }

  private def proof(query: Query): Receive = {
    case event: Event =>
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
      val flow = stage match {
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

      flow.foreach(_ => self ! Continue)
  }

  def receive = proof(query)

}
