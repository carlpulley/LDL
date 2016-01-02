package cakesolutions.monitor

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import cakesolutions.AkkaMonitoring
import cakesolutions.model.ModelGenerators
import cakesolutions.model.QueryModel.{Completed, Event, Next, StableValue}
import cakesolutions.syntax.QueryLanguage.GroundFact
import org.scalacheck.{Gen, Shrink}
import org.scalatest._
import org.scalatest.prop._

//class ExtensionTest
//  extends PropSpec
//  with PropertyChecks
//  with Matchers
//  with BeforeAndAfterAll
//  with ModelGenerators {
//
//  implicit val noShrink: Shrink[List[Event]] = Shrink.shrinkAny
//  implicit val system = ActorSystem("TestActorSystem")
//
//  val monitor = AkkaMonitoring(system)
//  val probe = TestProbe()
//
//  val TraceGen: Gen[List[Event]] =
//    for {
//      events <- Gen.listOf(Gen.containerOf[Set, GroundFact](GroundFactGen).map(Next(_, probe.ref)))
//      lastEvent <- Gen.containerOf[Set, GroundFact](GroundFactGen).map(Completed(_, probe.ref))
//    } yield events :+ lastEvent
//
//  property("[true] ff") {
//    forAll(TraceGen) { trace =>
//      val obs = monitor.query("""
//        |[true] FF
//        |""".stripMargin
//      )
//
//      trace.foreach(msg => obs.map(_ ! msg))
//
//      probe.expectMsg(StableValue(false))
//    }
//  }
//
//}
