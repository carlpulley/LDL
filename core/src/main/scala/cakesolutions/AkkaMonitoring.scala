package cakesolutions

import akka.actor._

object AkkaMonitoring extends ExtensionId[monitor.RuntimeMonitor] with ExtensionIdProvider {

  override def lookup = AkkaMonitoring

  override def createExtension(system: ExtendedActorSystem) = new monitor.RuntimeMonitor(system)

  override def get(system: ActorSystem): monitor.RuntimeMonitor = super.get(system)

}
