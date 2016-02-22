package cakesolutions

import akka.actor.{ActorContext, ActorRef, ActorSelection}
import cakesolutions.monitor.Model.{Monitor, Tell}

package object monitor {

  implicit final class ActorRefHook(ref: ActorRef) {
    @inline
    def `!+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref)
      val sender = context.sender()
      ref.tell(msg, sender)
    }

    @inline
    def `>+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref)
      ref.forward(msg)
    }
  }

  implicit final class ActorSelectionHook(ref: ActorSelection) {
    @inline
    def `!+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref)
      val sender = context.sender()
      ref.tell(msg, sender)
    }

    @inline
    def `>+`(msg: Any)(implicit monitor: Monitor, context: ActorContext): Unit = {
      monitor.ref ! Tell(msg, ref)
      ref.forward(msg)
    }
  }

}
