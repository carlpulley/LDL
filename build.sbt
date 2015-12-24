import Dependencies._

Build.Settings.project

name := "linear-dynamic-logic"

antlr4Settings

antlr4PackageName in Antlr4 := Some("cakesolutions.antlr4")

antlr4GenListener in Antlr4 := false
antlr4GenVisitor in Antlr4 := true

libraryDependencies ++= Seq(
  async,
  akka.actor,
  scalaz.core,
  akka.slf4j,
  logback,
  slf4j.simple,
  // Testing
  akka.testkit,
  scalatest  % "test",
  scalacheck % "test"
)
