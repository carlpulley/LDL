import sbt._
import Keys._

object Dependencies {

  object akka {
    val version = "2.4-M2"

    val actor = "com.typesafe.akka" %% "akka-actor" % version

    val testkit = "com.typesafe.akka" %% "akka-testkit" % version
  }

  val async = "org.scala-lang.modules" %% "scala-async" % "0.9.3"

  object scalaz {
    val version = "7.1.3"

    val core = "org.scalaz" %% "scalaz-core" % version
  }

  val typesafeConfig = "com.typesafe"   % "config"      % "1.3.0"

  // Testing
  val scalatest      = "org.scalatest"  %% "scalatest"  % "2.2.5"
  val scalacheck     = "org.scalacheck" %% "scalacheck" % "1.12.4"

}
