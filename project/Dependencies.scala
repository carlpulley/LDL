import sbt._

object Dependencies {

  object akka {
    val version = "2.4.1"

    val actor = "com.typesafe.akka"   %% "akka-actor"   % version
    val slf4j = "com.typesafe.akka"   %% "akka-slf4j"   % version

    val testkit = "com.typesafe.akka" %% "akka-testkit" % version
  }

  object scalaz {
    val version = "7.1.3"

    val core = "org.scalaz" %% "scalaz-core" % version
  }

  object slf4j {
    val version = "1.6.1"

    val simple = "org.slf4j" % "slf4j-simple" % version
    val api    = "org.slf4j" % "slf4j-api"    % version
  }

  val async          = "org.scala-lang.modules" %% "scala-async"              % "0.9.3"
  val logback        = "ch.qos.logback"         %  "logback-classic"          % "1.1.2"
  val reflection     = "org.scala-lang"         %  "scala-reflect"            % "2.11.7"
  val typesafeConfig = "com.typesafe"           %  "config"                   % "1.3.0"

  // Testing
  val scalatest      = "org.scalatest"  %% "scalatest"  % "2.2.5"
  val scalacheck     = "org.scalacheck" %% "scalacheck" % "1.12.4"

}
