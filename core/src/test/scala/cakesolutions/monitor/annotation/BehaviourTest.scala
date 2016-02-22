package cakesolutions.monitor.annotation

import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.event.LoggingReceive
import akka.testkit.TestProbe
import cakesolutions.monitor._
import org.scalatest.FreeSpec

import scala.concurrent.duration._

class BehaviourTest extends FreeSpec {

  implicit val system = ActorSystem("TestActorSystem")

  //@Behaviour("[ expectZero; (_?Int ; _!String)+ ] seenZero")
  @Behaviour("expectZero && [ (_?Int ; _!String)+ ] seenZero")
  class Int2StringActor(probe: TestProbe) extends Actor {

    role("expectZero")

    val seenZero: Receive = LoggingReceive {
      case msg: Int =>
        probe.ref !+ s"reply-$msg"
        role("seenZero")
    }

    val expectZero: Receive = LoggingReceive {
      case 0 =>
        probe.ref !+ "reply-0"
        context.become(seenZero)
        role("seenZero")
    }

    def receive = expectZero
  }

  "With `Query` session types" - {

    "Monitored actors permit allowable messages" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      ref.tell(0, sender.ref)
      probe.expectMsg("reply-0")
      supervisor.expectMsgPF(2.seconds) {
        case Terminated(actorRef) => false
        case _ => true
      }
    }

    "Monitored actors permit multiple allowable messages" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      for (n <- List(0, 5, 42, 1, 12, 0, -4)) {
        ref.tell(n, sender.ref)
        probe.expectMsg(s"reply-$n")
      }
      supervisor.expectMsgPF(2.seconds) {
        case Terminated(actorRef) => false
        case _ => true
      }
    }

    "Monitored actors stop with invalid states" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      ref.tell(42, sender.ref)
      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

    "Monitored actors stop with invalid messages" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      for (n <- List(0, 5, 42, 1, 12, 0, -4)) {
        ref.tell(n, sender.ref)
        probe.expectMsg(s"reply-$n")
      }

      ref.tell(4.2, sender.ref)
      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

  }

}
