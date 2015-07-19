package cakesolutions

import akka.actor._

object AkkaMonitoring extends ExtensionId[monitor.CVC4] with ExtensionIdProvider {

  override def lookup = AkkaMonitoring

  override def createExtension(system: ExtendedActorSystem) = new monitor.CVC4(system)

  override def get(system: ActorSystem): monitor.CVC4 = super.get(system)

}
