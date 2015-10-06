package cakesolutions.monitor

import akka.actor.{ActorRef, ExtendedActorSystem, Extension}
import cakesolutions.syntax.{QueryLanguage, QueryParser}
import com.codecommit.gll.Success

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
   * @return monitoring actor that events are to be sent to.
   */
  def query(formula: String): ActorRef = {
    // FIXME:
    extSystem.actorOf(CVC4Prover.props(QueryParser.Query(formula).head.asInstanceOf[Success[QueryLanguage.Query]].value))
  }

}
