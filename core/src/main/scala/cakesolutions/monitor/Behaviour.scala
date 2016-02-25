package cakesolutions.monitor

import akka.actor.{ActorContext, ActorRef, ActorSelection}
import akka.contrib.pattern.ReceivePipeline.{HandledCompletely, Inner}
import cakesolutions.model.QueryModel.{Notification, StableValue}
import cakesolutions.monitor.Model._
import cakesolutions.syntax.BehaviourParser
import cakesolutions.syntax.QueryLanguage.GroundFact

import scala.language.reflectiveCalls

object Behaviour {

  implicit final class ActorRefHook(ref: ActorRef) {
    @inline
    def `!+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      val sender = context.sender()
      monitor.ref.tell(Tell(msg, ref).event, sender)
      ref.tell(msg, sender)
    }

    @inline
    def `>+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref).event
      ref.forward(msg)
    }
  }

  implicit final class ActorSelectionHook(ref: ActorSelection) {
    @inline
    def `!+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      val sender = context.sender()
      monitor.ref.tell(Tell(msg, ref).event, sender)
      ref.tell(msg, sender)
    }

    @inline
    def `>+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref).event
      ref.forward(msg)
    }
  }

}

trait Behaviour extends akka.contrib.pattern.ReceivePipeline {
  this: { def query: String } =>

  private[this] lazy val behaviour = BehaviourParser.behaviour(query).get

  // TODO: incorporate monitor actor path into contextual logging?
  protected[this] implicit lazy val monitor = Monitor(context.system.actorOf(Model.props(self, behaviour)))

  @inline
  protected def role(observations: String*): Unit = {
    monitor.ref ! State(observations.map(obs => GroundFact(obs, Some("role"))).toSet).event
  }

  override def postStop(): Unit = {
    context.system.log.debug("Monitored actor is stopping")
    monitor.ref ! Completed.event
    super.postStop()
  }

  pipelineOuter {
    case msg @ StableValue(false) =>
      context.system.log.debug(s"Received monitor notification: $msg")
      context.system.log.error("Monitor has falsified query - stopping actor!")
      context.stop(self)
      HandledCompletely

    case msg: Notification =>
      context.system.log.debug(s"Received monitor notification: $msg")
      HandledCompletely

    case msg: Any =>
      try {
        val realSender = sender()
        monitor.ref ! Receive(msg, realSender).event
        Inner(msg)
      } catch {
        case scala.util.control.NonFatal(exn) =>
          context.system.log.error(exn, "Failed to forward received message to monitor - stopping actor!")
          context.stop(self)
          HandledCompletely
      }
  }

}
