package cakesolutions.monitor.annotation

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.testkit.TestProbe
import cakesolutions.monitor.Behaviour
import org.scalatest.FreeSpec

class BehaviourTest extends FreeSpec {

  implicit val system = ActorSystem("TestActorSystem")

  class Int2StringActor(probe: TestProbe) extends Actor with Behaviour {

    import Behaviour._

    val query = "expectZero && [ (_?Integer ; _!String)+ ] seenZero"

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
      supervisor.expectNoMsg()
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
      supervisor.expectNoMsg
    }

    "Monitored actors stop with invalid message types" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      ref.tell('w', sender.ref)
      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

    "Monitored actors eventually stop with invalid message types" in {
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

    // FIXME: develop strategy for monitoring unhandled messages!
    "Monitored actors evetually stop with correctly typed unhandled messages" in {
      val probe = TestProbe()
      val sender = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      // Unhandled message
      ref.tell(42, sender.ref)
      // Handled message
      ref.tell(0, sender.ref)

      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

  }

}
