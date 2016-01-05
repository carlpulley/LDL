package cakesolutions.monitor

import akka.actor._
import cakesolutions.syntax.QueryLanguage.Query
import cakesolutions.syntax.QueryParser

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class RuntimeMonitor(system: ExtendedActorSystem) extends Extension {

  def actorOf[T : ClassTag](props: Props)(sessionType: String): Try[ActorRef] = {
    Setup.checker(sessionType).map { ref =>
      Setup.probe[T](props, ref)(sessionType)
    }
  }

  def actorOf[T : ClassTag](props: Props, name: String)(sessionType: String): Try[ActorRef] = {
    Setup.checker(sessionType).map { ref =>
      Setup.probe[T](props, ref, name)(sessionType)
    }
  }

  object Setup {

    def probe[T : ClassTag](props: Props, monitor: ActorRef)(sessionType: String): ActorRef =
      system.actorOf(Probe.props[T](props, monitor, sessionType))

    def probe[T : ClassTag](props: Props, monitor: ActorRef, name: String)(sessionType: String): ActorRef =
      system.actorOf(Probe.props[T](props, monitor, sessionType), name)

    // TODO: need to extract query from `sessionType`
    def checker(sessionType: String): Try[ActorRef] = {
      QueryParser.query(sessionType) match {
        case Success(query: Query) =>
          Success(system.actorOf(Model.props(query)))

        case Failure(exn) =>
          Failure(exn)
      }
    }

  }

}
