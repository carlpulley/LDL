package cakesolutions.application

import cakesolutions.application.ConfigUtils.ConfigValidationFailure
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.scalatest.FreeSpec

import scala.concurrent.duration._
import scalaz.{-\/, \/-}

object ConfigUtilsTest {
  case object GenericTestFailure extends ConfigValidationFailure
  case object NameShouldBeNonEmptyAndLowerCase extends ConfigValidationFailure
  case object ShouldBePositive extends ConfigValidationFailure
  case object ShouldNotBeNegative extends ConfigValidationFailure

  final case class HttpConfig(host: String, port: Int)
  final case class Settings(name: String, timeout: FiniteDuration, http: HttpConfig)
}

class ConfigUtilsTest extends FreeSpec {

  import ConfigUtils._
  import ConfigUtilsTest._

  "parameter checking" - {
    implicit val config = ConfigFactory.parseString(
      """
        |top-level-name = "test"
        |test {
        |  nestedVal = 50.68
        |  nestedDuration = 4 h
        |  nestedList = []
        |  context {
        |    valueInt = 30
        |    valueStr = "test string"
        |    valueDuration = 12 ms
        |    valueStrList = [ "addr1:10", "addr2:20", "addr3:30" ]
        |    valueDoubleList = [ 10.2, 20, 0.123 ]
        |  }
        |}
      """.stripMargin)

    "validate methods" in {
      assert(validate[String]("top-level-name", GenericTestFailure)("test" == _) == \/-("test"))
      assert(validate[Double]("test.nestedVal", GenericTestFailure)(50.68 == _) == \/-(50.68))
      assert(validate[FiniteDuration]("test.nestedDuration", GenericTestFailure)(4.hours == _) == \/-(4.hours))
      assert(validate[List[Double]]("test.nestedList", GenericTestFailure)(_.isEmpty) == \/-(List.empty[Double]))
      assert(validate[Int]("test.context.valueInt", GenericTestFailure)(30 == _) == \/-(30))
      assert(validate[String]("test.context.valueStr", GenericTestFailure)("test string" == _) == \/-("test string"))
      assert(validate[FiniteDuration]("test.context.valueDuration", GenericTestFailure)(12.milliseconds == _) == \/-(12.milliseconds))
      assert(validate[List[String]]("test.context.valueStrList", GenericTestFailure)(List("addr1:10", "addr2:20", "addr3:30") == _) == \/-(List("addr1:10", "addr2:20", "addr3:30")))
      assert(validate[List[Double]]("test.context.valueDoubleList", GenericTestFailure)(List(10.2, 20, 0.123) == _) == \/-(List(10.2, 20, 0.123)))

      assert(validate[String]("top-level-name", GenericTestFailure)(_ => false) == -\/(ValueFailure("top-level-name", GenericTestFailure)))
      validate[String]("invalid-path", GenericTestFailure)(_ => true) match {
        case -\/(ValueFailure("invalid-path", NullValue)) =>
          assert(true)
        case result =>
          assert(false, result)
      }
      validate[String]("test.invalid-path", GenericTestFailure)(_ => true) match {
        case -\/(ValueFailure("test.invalid-path", NullValue)) =>
          assert(true)
        case result =>
          assert(false, result)
      }
      validate[Int]("top-level-name", GenericTestFailure)(_ => true) match {
        case -\/(ValueFailure("top-level-name", InvalidValueType(_))) =>
          assert(true)
        case result =>
          assert(false, result)
      }
    }

    "unchecked methods" in {
      assert(unchecked[String]("top-level-name") == \/-("test"))
      assert(unchecked[Double]("test.nestedVal") == \/-(50.68))
      assert(unchecked[FiniteDuration]("test.nestedDuration") == \/-(4.hours))
      assert(unchecked[List[Double]]("test.nestedList") == \/-(List.empty[Double]))
      assert(unchecked[Int]("test.context.valueInt") == \/-(30))
      assert(unchecked[String]("test.context.valueStr") == \/-("test string"))
      assert(unchecked[FiniteDuration]("test.context.valueDuration") == \/-(12.milliseconds))
      assert(unchecked[List[String]]("test.context.valueStrList") == \/-(List("addr1:10", "addr2:20", "addr3:30")))
      assert(unchecked[List[Double]]("test.context.valueDoubleList") == \/-(List(10.2, 20, 0.123)))

      unchecked[String]("invalid-path") match {
        case -\/(ValueFailure("invalid-path", NullValue)) =>
          assert(true)
        case result =>
          assert(false, result)
      }
      unchecked[String]("test.invalid-path") match {
        case -\/(ValueFailure("test.invalid-path", NullValue)) =>
          assert(true)
        case result =>
          assert(false, result)
      }
      unchecked[Int]("top-level-name") match {
        case -\/(ValueFailure("top-level-name", InvalidValueType(_))) =>
          assert(true)
        case result =>
          assert(false, result)
      }
    }
  }

  "Ensure config files may be correctly parsed and validated with environment variable overrides" in {
    val envMapping = ConfigFactory.parseString(
      """
        |env {
        |  AKKA_HOST = docker-local
        |  AKKA_PORT = 2552
        |  AKKA_BIND_HOST = google.co.uk
        |  AKKA_BIND_PORT = 123
        |
        |  HTTP_ADDR = 192.168.99.100
        |  HTTP_PORT = 5678
        |}
      """.stripMargin
    )
    implicit val config = ConfigFactory.parseResourcesAnySyntax("application.conf").withFallback(envMapping).resolve()

    val validatedConfig =
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

    assert(validatedConfig.isRight)
    validatedConfig match {
      case \/-(Settings("example1", timeout, HttpConfig("192.168.99.100", 5678))) =>
        assert(timeout == 30.seconds)
    }
  }

}
