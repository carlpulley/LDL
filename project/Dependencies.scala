import sbt._
import Keys._

object Dependencies {

  object akka {
    val version = "2.4.4"

    // Core Akka
    val actor                 = "com.typesafe.akka"      %% "akka-actor"                    % version
    val cluster               = "com.typesafe.akka"      %% "akka-cluster"                  % version
    val contrib               = "com.typesafe.akka"      %% "akka-contrib"                  % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version

    object http {
      val api      = "com.typesafe.akka" %% "akka-http-experimental"           % version
      val core = "com.typesafe.akka" %% "akka-http-core-experimental"      % version
    }

    object persistence {
      val cassandra = "com.github.krasserm" %% "akka-persistence-cassandra" % "0.3.4"
      val core = "com.typesafe.akka" %% "akka-persistence-experimental" % version
    }

    object streams {
      val core      = "com.typesafe.akka" %% "akka-stream-experimental"         % version
      val testkit   = "com.typesafe.akka" %% "akka-stream-testkit-experimental" % version
    }

    val testkit               = "com.typesafe.akka"      %% "akka-testkit"                  % version
  }

  object scalaz {
    val core = "org.scalaz" %% "scalaz-core" % "7.3.0-M2"
  }

  object slf4j {
    val version = "1.7.21"

    val api        = "org.slf4j"              % "slf4j-api"    % version
    val simple     = "org.slf4j"              % "slf4j-simple" % version
  }

  val ficus = "com.iheart" %% "ficus" % "1.2.5"
  //val async            = "org.scala-lang.modules" %% "scala-async"  % "0.9.3"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"
  //val parboiled        = "org.parboiled"          %% "parboiled"    % "2.0.1"
  val scalacheck       = "org.scalacheck"         %% "scalacheck"   % "1.12.1"
  val scalatest        = "org.scalatest"          %% "scalatest"    % "2.2.1"
  //val scodec_bits      = "org.typelevel"          %% "scodec-bits"  % "1.0.4"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.1"
  val typesafeConfig   = "com.typesafe"           % "config"        % "1.2.1"

}
