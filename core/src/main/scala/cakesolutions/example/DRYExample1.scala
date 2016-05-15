package cakesolutions.example

import java.util.concurrent.Executors

import cakesolutions.application.ApplicationFSM._
import cakesolutions.application.ConfigUtils.ConfigValidationFailure
import cakesolutions.application.{ConfigUtils, ConfiguredApplication}
import cakesolutions.logging.ContextualLogging.Context
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Random
import scalaz.{-\/, \/, \/-}

case object ExampleFailure extends ApplicationFSMError

case object ShouldBePositive extends ConfigValidationFailure
case object ShouldNotBeNegative extends ConfigValidationFailure
case object NameShouldBeNonEmptyAndLowerCase extends ConfigValidationFailure

final case class HttpConfig(host: String, port: Int)
final case class Settings(name: String, timeout: FiniteDuration, http: HttpConfig)

object DRYExample1 extends ConfiguredApplication[Settings] {
  import ConfigUtils._
  import ConfiguredApplication._

  protected implicit lazy val context = Context(applicationKey -> "example-1")

  override protected lazy val threadPool = Executors.newCachedThreadPool()

  private[this] lazy val rand = Random

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

  override def workload(
    config: Settings
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
      log.info("Application never returns!")()
      // Work is done forever!
    }
  }
}
