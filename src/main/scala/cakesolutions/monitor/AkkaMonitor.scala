package cakesolutions.monitor

import akka.actor.{ActorRef, ExtendedActorSystem, Extension}
import akka.io.UdpConnected.Event
import cakesolutions.syntax.QueryLanguage.Query
import cakesolutions.syntax.QueryParser

class AkkaMonitor(system: ExtendedActorSystem) extends Extension {

  /**
   * Factory method for creating LDL query monitors.
   *
   * e.g. {{{
   *      monitor """
   *        [true] ff
   *      """ using probe
   * }}}
   *
   * @param query query that is to be monitored (in real-time).
   * @param probe monitoring notifications (in response to events) are sent to this actor.
   * @return cancellable instance.
   */
  def monitor(query: String): { def using(probe: ActorRef): { def cancel(): Boolean } } = {
    val parsedQuery = new QueryParser(query).Query.run().get // FIXME: to throw an exception or not?
    new MonitorWith(parsedQuery)
  }

  private class MonitorWith private[monitor] (query: Query) {

    private[this] var monitor: Option[ActorRef] = Option.empty

    /**
     * Method to terminate query monitoring.
     *
     * @return true iff monitor was successfully terminated.
     */
    def cancel(): Boolean = {
      val result = monitor.forall(system.eventStream.unsubscribe(_, classOf[Event]))
      monitor.foreach(system.stop)
      monitor = Option.empty
      result
    }

    /**
     * Add an actor probe to this monitoring instance. All monitored notifications will be sent to this probe actor.
     *
     * @param probe monitoring notifications (in response to events) are sent to this actor.
     * @return cancellable instance.
     */
    def using(probe: ActorRef): { def cancel(): Boolean } = {
      monitor = Some(system.actorOf(CVC4Prover.props(query)))
      monitor.foreach(system.eventStream.subscribe(_, classOf[Event])) // FIXME: need to scope per CVC4 instance!

      this
    }

  }

}
