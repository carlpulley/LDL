import sbt.Keys._
import sbt.{Build => _, _}

object Build {

  val SNAPSHOT = true
  val RELEASE  = false

  object Settings {

    val root: Seq[Setting[_]] = Defaults.defaultConfigs ++ Seq(
      version in ThisBuild := Build.generateVersion("0", "1", SNAPSHOT)
    )
    
    val project = 
        Defaults.defaultConfigs ++ 
        Defaults.itSettings ++
        BaseSettings.projectSettings ++
        Classpaths.ivyPublishSettings ++ 
        Classpaths.jvmPublishSettings
  }

  def generateVersion(major: String, minor: String, snapshot: Boolean = RELEASE) = {
    // Include the git version sha in the build version for repeatable historical builds.
    val gitHeadCommitSha = settingKey[String]("current git commit SHA")
    val incremental = Process("git rev-parse HEAD").lines.head
    s"$major.$minor.$incremental${if (snapshot) "-SNAPSHOT"}"
  }

}
