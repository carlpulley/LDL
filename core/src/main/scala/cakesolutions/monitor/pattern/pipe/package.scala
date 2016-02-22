package cakesolutions.monitor.pattern

import akka.actor.{ActorContext, ActorRef, ActorSelection}
import cakesolutions.monitor.Model.Tell

import scala.concurrent.Future

package object pipe {
  import akka.pattern.pipe

  implicit final class PipeToHook[T](msg: Future[T]) {
    @inline
    def `|+`(ref: ActorRef)(implicit monitor: ActorRef, context: ActorContext): Future[T] = {
      import context.dispatcher

      val sender = context.sender()
      msg.map(Tell(_, sender)).pipeTo(monitor)(sender).flatMap { _ =>
        msg.pipeTo(ref)(sender)
      }
    }

    @inline
    def `|+`(ref: ActorSelection)(implicit monitor: ActorRef, context: ActorContext): Future[T] = {
      import context.dispatcher

      val sender = context.sender()
      msg.map(Tell(_, sender)).pipeTo(monitor)(sender).flatMap { _ =>
        msg.pipeToSelection(ref)(sender)
      }
    }
  }

}
