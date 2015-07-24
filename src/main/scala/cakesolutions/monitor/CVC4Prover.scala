package cakesolutions.monitor

import akka.actor.{Actor, ActorLogging}
import cakesolutions.model.provers.CVC4
import cakesolutions.syntax.QueryLanguage.{Query, GroundFact}
import com.typesafe.config.ConfigFactory

class CVC4Prover(query: Query) extends Actor with ActorLogging {

  type Observation = Set[GroundFact]

  val cvc4 = new CVC4(ConfigFactory.load("prover.conf"))

  def receive = {
    case obs: Observation =>
      ??? // FIXME:
  }

}
