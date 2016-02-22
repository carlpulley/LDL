package cakesolutions.monitor.pattern

import akka.actor.{ActorContext, ActorRef, ActorSelection}
import akka.util.Timeout
import cakesolutions.monitor.Model.{Monitor, Receive, Tell}

import scala.concurrent.Future

package object ask {
  import akka.pattern.ask

  implicit final class ActorRefAskHook(ref: ActorRef) {
    @inline
    def `?+`(msg: Any)(implicit monitor: Monitor, timeout: Timeout, context: ActorContext): Future[Any] = {
      val sender = context.sender()
      monitor.ref ! Tell(msg, sender.path)
      ref.ask(msg)(timeout, sender).map { reply =>
        monitor.ref ! Receive(reply, ref)
        reply
      } (context.dispatcher)
    }
  }

  implicit final class ActorSelectionAskHook(ref: ActorSelection) {
    @inline
    def `?+`(msg: Any)(implicit monitor: Monitor, timeout: Timeout, context: ActorContext): Future[Any] = {
      val sender = context.sender()
      monitor.ref ! Tell(msg, sender.path)
      ref.ask(msg)(timeout, sender).map { reply =>
        monitor.ref ! Receive(reply, ref)
        reply
      } (context.dispatcher)
    }
  }

}
