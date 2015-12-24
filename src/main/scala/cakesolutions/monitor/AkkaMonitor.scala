package cakesolutions.monitor

import akka.actor.{ActorRef, ExtendedActorSystem, Extension}
import cakesolutions.syntax.QueryParser
import cakesolutions.syntax.QueryLanguage.Query

import scala.util.Success

class AkkaMonitor(extSystem: ExtendedActorSystem) extends Extension {

  /**
   * Factory method for creating LDL query monitors. Monitoring notifications are sent to event actor sender.
   *
   * e.g. {{{
   *      query """
   *        [true] ff
   *      """
   * }}}
   *
   * @param formula query that is to be monitored, on this actor system, in real-time.
   * @return (optional) monitoring actor that events are to be sent to.
   */
  def query(formula: String): Option[ActorRef] = {
    QueryParser.query(formula) match {
      case Success(query: Query) =>
        Some(extSystem.actorOf(CVC4Prover.props(query)))

      case _ =>
        None
    }
  }

}
