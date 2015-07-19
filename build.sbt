import Dependencies._

Build.Settings.project

name := "linear-dynamic-logic"

libraryDependencies ++= Seq(
  async,
  akka.actor,
  scalaz.core,
  // Testing
  scalatest  % "test",
  scalacheck % "test"
)
