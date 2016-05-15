import Dependencies._
import sbt.Keys._
import sbt._

name := "linear-dynamic-logic"

lazy val core = Project(
  id = "core",
  base = file("core"),
  settings = CommonProject.settings ++ Seq(
    target in javah in Compile := (sourceDirectory in native).value / "include",
    libraryDependencies ++= Seq(
      ficus,
      logback,
      scalacheck,
      scalatest,
      scalaz.core,
      shapeless,
      slf4j.simple,
      typesafeConfig
    )
  ),
  dependencies = Seq(
    native % Runtime
  )
).enablePlugins(JniLoading)

lazy val native = Project(
  id = "native",
  base = file("native"),
  settings = CommonProject.settings ++ Seq(
    //enableNativeCompilation in Compile := false,
    sourceDirectory in nativeCompile in Compile := sourceDirectory.value,
    nativeLibraryPath in Compile := "/ch/jodersky/jni/basic/native"
  )
)//.enablePlugins(JniNative)

lazy val root = Project(
  id = "root",
  base = file("."),
  aggregate = Seq(core, native)
)
