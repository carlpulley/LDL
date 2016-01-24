package cakesolutions.monitor.annotation

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestProbe
import org.scalatest.FreeSpec

class BehaviourTest extends FreeSpec {

  implicit val system = ActorSystem("TestActorSystem")

  @Behaviour("expectZero && [ (_?Int ; _!String)+ ] seenZero")
  class Int2StringActor(probe: TestProbe) extends Actor {
    // @Role
    val seenZero: Receive = {
      case msg: Int =>
        probe.ref ! msg.toString // @Event
    }

    // @Role
    val expectZero: Receive = {
      case 0 =>
        probe.ref ! "0" // @Event
        context.become(seenZero)
    }

    def receive = expectZero
  }

  "With `Query` session types" - {

    "Monitored actors permit allowable messages" in {
      val probe = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      ref ! 0
      probe.expectMsg("0")
    }

    "Monitored actors permit multiple allowable messages" in {
      val probe = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      for (n <- List(0, 5, 42, 1, 12, 0, -4)) {
        ref ! n
        probe.expectMsg(n.toString)
      }
    }

    "Monitored actors stop with invalid states" in {
      val probe = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      ref ! 42
      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

    "Monitored actors stop with invalid messages" in {
      val probe = TestProbe()
      val supervisor = TestProbe()
      val ref = system.actorOf(Props(new Int2StringActor(probe)))

      supervisor.watch(ref)

      for (n <- List(0, 5, 42, 1, 12, 0, -4)) {
        ref ! n
        probe.expectMsg(n.toString)
      }

      ref ! 4.2
      probe.expectNoMsg()
      supervisor.expectTerminated(ref)
    }

  }

}
