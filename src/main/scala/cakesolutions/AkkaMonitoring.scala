package cakesolutions

import akka.actor._

object AkkaMonitoring extends ExtensionId[monitor.AkkaMonitor] with ExtensionIdProvider {

  override def lookup = AkkaMonitoring

  override def createExtension(system: ExtendedActorSystem) = new monitor.AkkaMonitor(system)

  override def get(system: ActorSystem): monitor.AkkaMonitor = super.get(system)

}
