package cakesolutions.monitor
package model

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.Inner
import cakesolutions.model.QueryModel
import cakesolutions.monitor.Model._
import cakesolutions.syntax.QueryLanguage.{Observation => _, _}

object EventExtractor {

  def translate(obs: Observation): QueryModel.Event = obs match {
    case Receive(msg, sender) =>
      QueryModel.Next(Set(Neg(GroundFact(msg.getClass.getSimpleName, messageType)), Neg(GroundFact(sender.toSerializationFormat, nameType))))

    case Tell(msg, recipient) =>
      QueryModel.Next(Set(GroundFact(msg.getClass.getSimpleName, messageType), GroundFact(recipient.toSerializationFormat, nameType)))

    case State(observations) =>
      QueryModel.Next(observations)

    case Completed =>
      QueryModel.Completed(Set.empty)
  }

}

private[monitor] trait EventExtractor extends ReceivePipeline {

  import EventExtractor._

  pipelineOuter {
    case msg: Model.Observation =>
      context.system.log.debug(s"Received message $msg")
      Inner(translate(msg))
  }

}
