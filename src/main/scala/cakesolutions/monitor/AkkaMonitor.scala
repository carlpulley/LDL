package cakesolutions.monitor

import akka.actor.{ActorRef, Props, Extension, ExtendedActorSystem}
import cakesolutions.syntax.QueryLanguage.Query
import java.util.UUID
import cakesolutions.syntax.{QueryLanguage, QueryParser}

import scala.collection.mutable
import scala.util.Try

object AkkaMonitor {
  private var probes = mutable.Map.empty[UUID, ActorRef]
}

class AkkaMonitor(system: ExtendedActorSystem) extends Extension {

  import AkkaMonitor._
  import QueryLanguage.Observation

  private[this] def monitor(query: Query, probe: ActorRef): UUID = {
    val id = UUID.randomUUID()
    val monitor = system.actorOf(Props(new CVC4Prover(query))) // TODO: should we be using a stream here?
    probes += id -> monitor

    system.eventStream.subscribe(monitor, classOf[Observation])

    id
  }

  /**
   * Factory method for creating LDL query monitors.
   *
   * e.g. {{{
   *      monitor """
   *        [true] ff
   *      """ using actorRef
   * }}}
   *
   * @param query query that is to be monitored (in real-time).
   * @param probe monitoring events are sent to this actor.
   * @return unique identifier (i.e. UUID) for the monitor.
   */
  def monitor(query: String): MonitorWith = {
    new MonitorWith(query)
  }

  /**
   * Method to terminate query monitoring.
   *
   * @param id unique identifier (i.e. UUID) for monitor that is to be terminated.
   * @return true iff monitor was successfully terminated.
   */
  def cancel(id: UUID): Boolean = {
    if (probes.contains(id)) {
      val result = system.eventStream.unsubscribe(probes(id), classOf[Observation])
      system.stop(probes(id))
      probes -= id

      result
    } else {
      false
    }
  }

  class MonitorWith private[monitor] (query: String) {
    def using(probe: ActorRef): Try[UUID] = {
      for {
        form <- new QueryParser(query).Query.run()
      } yield monitor(form, probe)
    }
  }

}
