package cakesolutions.monitor

import akka.actor.{ActorRef, Terminated}
import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.HandledCompletely

private[monitor] trait WatchSubject extends ReceivePipeline {

  def subject: ActorRef

  context.watch(subject)

  pipelineOuter {
    case Terminated(ref) if ref == subject =>
      context.system.log.error(s"Model has terminated - terminating $self")
      context.stop(self)
      HandledCompletely
  }

}
