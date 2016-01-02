package cakesolutions.monitor

import akka.actor._
import akka.event.LoggingReceive
import cakesolutions.model.QueryModel.{ObservableEvent, StableValue}
import cakesolutions.syntax.QueryLanguage._

import scala.reflect.ClassTag

/**
 * Internal API
 */
object Probe {

  case class InvalidMessageType(reason: String) extends Exception

  private[monitor] def props[T : ClassTag](props: Props, monitor: ActorRef, sessionType: String) =
    Props(new Probe[T](props, monitor, sessionType))

}

/**
 * TODO: document!
 */
// TODO: setup Probe actor as being persistent!
class Probe[T : ClassTag](props: Props, monitor: ActorRef, sessionType: String) extends Actor with ActorLogging {

  import Probe._

  private[this] val wrappedActor = context.actorOf(props)
  // TODO: determine a suitable strategy for extracting partial function from type `T` and `sessionType`
  private[this] val transform: PartialFunction[T, Observation] = {
    case _: T =>
      ???
  }

  override def preStart(): Unit = {
    context.watch(monitor)
    context.watch(wrappedActor)
  }

  override def postStop(): Unit = {
    context.stop(monitor)
    context.stop(wrappedActor)
  }

  def receive: Receive = LoggingReceive {
    case Terminated(`monitor`) | Terminated(`wrappedActor`) =>
      log.error(s"Deathwatch detected that one of $monitor or $wrappedActor had stopped")
      context.stop(self)
      
    case StableValue(false) =>
      log.error(s"Stopping $self - runtime monitor falsified session type $sessionType")
      context.stop(self)

    case _: ObservableEvent =>
      // Intentionally ignore all other observable events

    case msg: T if transform.isDefinedAt(msg) =>
      wrappedActor.tell(msg, sender())
      monitor ! transform(msg)

    case msg: T =>
      throw InvalidMessageType(s"Correctly typed message received, but the Probe's message transform is not defined at $msg")

    case _: Any =>
      throw InvalidMessageType("Incorrectly typed message received")
  }

}
