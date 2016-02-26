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

    val query = "expectZero && < (_?Integer ; _!String)+ > seenZero"

    role("expectZero")

    val seenZero: Receive = LoggingReceive {
      case msg: Integer =>
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

    "Monitored actors" - {
      "permit allowable messages" in {
        val probe = TestProbe()
        val sender = TestProbe()
        val supervisor = TestProbe()
        val ref = system.actorOf(Props(new Int2StringActor(probe)))

        supervisor.watch(ref)

        ref.tell(0, sender.ref)
        probe.expectMsg("reply-0")
        supervisor.expectNoMsg()
      }

      "permit multiple allowable messages" in {
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
    }

    "Monitored actors eventually stop" - {
      for ((msgType, msg) <- Set("Character" -> 'w', "Double" -> 4.2)) {
        s"with an invalid $msgType message type" in {
          val probe = TestProbe()
          val sender = TestProbe()
          val supervisor = TestProbe()
          val ref = system.actorOf(Props(new Int2StringActor(probe)))

          supervisor.watch(ref)

          ref.tell(msg, sender.ref)
          probe.expectNoMsg()
          sender.expectNoMsg()
          supervisor.expectTerminated(ref)
        }

        s"with multiple invalid $msgType message types" in {
          val probe = TestProbe()
          val sender = TestProbe()
          val supervisor = TestProbe()
          val ref = system.actorOf(Props(new Int2StringActor(probe)))

          supervisor.watch(ref)

          for (n <- List(0, 5, 42, 1, 12, 0, -4)) {
            ref.tell(n, sender.ref)
            probe.expectMsg(s"reply-$n")
          }

          ref.tell(msg, sender.ref)
          probe.expectNoMsg()
          sender.expectNoMsg()
          supervisor.expectTerminated(ref)
        }
      }
    }

  }

}
