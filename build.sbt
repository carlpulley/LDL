import Dependencies._

Build.Settings.project

name := "linear-dynamic-logic"

libraryDependencies ++= Seq(
  async,
  akka.actor,
  gll,
  scalaz.core,
  akka.slf4j,
  logback,
  slf4j.simple,
  // Testing
  akka.testkit,
  scalatest  % "test",
  scalacheck % "test"
)
