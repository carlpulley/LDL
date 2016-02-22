package cakesolutions.monitor

import akka.actor._
import cakesolutions.monitor.model.{EventExtractor, Prover, WatchSubject}
import cakesolutions.syntax.QueryLanguage.{Fact, Query}

object Model {

  /**
   * Internal API
   */
  sealed trait Observation

  final case class Receive(msg: Any, sender: ActorPath) extends Observation {
    def this(msg: Any, sender: ActorRef) = {
      this(msg, sender.path)
    }

    def this(msg: Any, sender: ActorSelection) = {
      this(msg, sender.anchorPath)
    }
  }
  object Receive {
    def apply(msg: Any, sender: ActorRef): Receive = {
      apply(msg, sender.path)
    }

    def apply(msg: Any, sender: ActorSelection): Receive = {
      apply(msg, sender.anchorPath)
    }
  }

  final case class Tell(msg: Any, recipient: ActorPath) extends Observation {
    def this(msg: Any, recipient: ActorRef) = {
      this(msg, recipient.path)
    }

    def this(msg: Any, recipient: ActorSelection) = {
      this(msg, recipient.anchorPath)
    }
  }
  object Tell {
    def apply(msg: Any, recipient: ActorRef): Tell = {
      apply(msg, recipient.path)
    }

    def apply(msg: Any, recipient: ActorSelection): Tell = {
      apply(msg, recipient.anchorPath)
    }
  }

  final case class State(observations: Set[Fact]) extends Observation

  case object Completed extends Observation

  /**
   * Internal API
   */
  final case class Monitor(ref: ActorRef) extends AnyVal

  def props(subject: ActorRef, query: Query) = Props(new Model(subject, query)).withDispatcher("prover.dispatcher")

}

final case class Model private (subject: ActorRef, query: Query) extends EventExtractor with WatchSubject with Prover with ActorLogging
