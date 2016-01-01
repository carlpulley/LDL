import sbt._

name := "linear-dynamic-logic"

lazy val root = Project(id = "root", base = file(".")).aggregate(core, benchmark)

lazy val core = Project(id = "core", base = file("core"))

lazy val benchmark = Project(id = "benchmark", base = file("benchmark")).dependsOn(core % "compile->test")
