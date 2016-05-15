package cakesolutions.example

import java.util.concurrent.Executors

import cakesolutions.application.ConfigUtils._
import cakesolutions.application._
import cakesolutions.logging.ContextualLogging.Context
import net.ceedubs.ficus.Ficus._
import shapeless.HList

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Random
import scalaz.{-\/, \/, \/-}

object DRYExample2 extends ConfiguredMicroService[Settings] {
  import ApplicationFSM._
  import ConfiguredMicroService._

  protected implicit lazy val context = Context(applicationKey -> "example-2")

  override protected lazy val threadPool = Executors.newCachedThreadPool()

  private[this] lazy val rand = Random

  override def serviceWait(
    config: Settings
  )(attempt: RetryAttempt[State]
  )(implicit callback: Promise[ApplicationFSMError \/ State]
  ): Unit = {

  }

  override lazy val validatedConfig = {
    validateConfig("application.conf") { implicit config =>
      build[Settings](
        validate[String]("name", NameShouldBeNonEmptyAndLowerCase)(_.matches("[a-z0-9_-]+")),
        validate[FiniteDuration]("http.timeout", ShouldNotBeNegative)(_ >= 0.seconds),
        via("http") { implicit config =>
          build[HttpConfig](
            unchecked[String]("host"),
            validate[Int]("port", ShouldBePositive)(_ > 0)
          )
        }
      )
    }
  }

  // TODO: demo retries due to loss of service in workload
  override def workload(
    config: Settings,
    services: HList
  )(implicit callback: Promise[ApplicationFSMError \/ State]
  ): Unit = {
    val die = math.abs(rand.nextInt()) % 5

    if (die == 0) {
      callback.success(-\/(ExampleFailure))
    } else if (die == 1) {
      callback.failure(new RuntimeException("Fake exception"))
    } else if (die == 2) {
      callback.success(\/-(Result("success")))
    } else {
      log.info("Application never fails")()
      // Work is done!
    }
  }
}
