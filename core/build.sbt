import Dependencies._

ProjectBuild.Settings.project

name := "linear-dynamic-logic-core"

antlr4Settings

antlr4PackageName in Antlr4 := Some("cakesolutions.antlr4")

antlr4GenListener in Antlr4 := false
antlr4GenVisitor in Antlr4 := true

libraryDependencies ++= Seq(
  async,
  akka.actor,
  akka.contrib,
  scalaz.core,
  akka.slf4j,
  logback,
  reflection,
  slf4j.simple,
  // Testing
  akka.testkit,
  scalatest  % "test",
  scalacheck % "test"
)
