package cakesolutions.application

import cakesolutions.application.ApplicationFSM._
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import shapeless._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
import scalaz.syntax.ToValidationOps
import scalaz.{-\/, \/, \/-}

object ConfigUtils extends ToValidationOps {
  trait ConfigValidationFailure
  case class InvalidValueType[Value](reason: Throwable) extends ConfigValidationFailure
  case object MissingValue extends ConfigValidationFailure
  case object NullValue extends ConfigValidationFailure

  sealed trait ValueError
  final case class NestedConfigError(config: ConfigError) extends ValueError
  final case class ValueFailure(path: String, reason: ConfigValidationFailure) extends ValueError

  final case class ConfigError(errors: ValueError*) extends ApplicationFSMError

  implicit def innerConfigValue[ConfigValue](config: ConfigError \/ ConfigValue): ValueError \/ ConfigValue = {
    config.leftMap(NestedConfigError)
  }

  def via[ConfigValue](path: String)(inner: Config => ConfigError \/ ConfigValue)(implicit config: Config): ValueError \/ ConfigValue = {
    inner(config.getConfig(path))
  }

  def build[ValidConfig](
    validatedParams: (ValueError \/ Any)*
  )(implicit gen: Generic[ValidConfig]
  ): ConfigError \/ ValidConfig = {
    val failuresHList = validatedParams.foldRight[(List[ValueError], HList)]((Nil, HNil)) {
      case (-\/(error), (failures, result)) =>
        (failures :+ error, result)
      case (\/-(value), (failures, result)) =>
        (failures, value :: result)
    }

    failuresHList match {
      case (Nil, result: gen.Repr) =>
        \/-(gen.from(result))
      case (failures, _) =>
        -\/(ConfigError(failures: _*))
    }
  }

  def unchecked[Value](
    path: String
  )(implicit config: Config,
    reader: ValueReader[Value]
  ): ValueFailure \/ Value = {
    Try(config.hasPath(path)) match {
      case Success(true) =>
        Try(config.as[Value](path)) match {
          case Success(value) =>
            \/-(value)
          case Failure(exn) =>
            -\/(ValueFailure(path, InvalidValueType[Value](exn)))
        }
      case Success(false) =>
        -\/(ValueFailure(path, NullValue))
      case Failure(_) =>
        -\/(ValueFailure(path, MissingValue))
    }
  }

  def validate[Value](
    path: String,
    failureReason: ConfigValidationFailure
  )(check: Value => Boolean
  )(implicit config: Config,
    reader: ValueReader[Value]
  ): ValueFailure \/ Value = {
    Try(config.hasPath(path)) match {
      case Success(true) =>
        Try(config.as[Value](path)) match {
          case Success(value) =>
            Try(check(value)) match {
              case Success(true) =>
                \/-(value)
              case Success(false) =>
                -\/(ValueFailure(path, failureReason))
              case Failure(exn) =>
                -\/(ValueFailure(path, InvalidValueType[Value](exn)))
            }
          case Failure(exn) =>
            -\/(ValueFailure(path, InvalidValueType[Value](exn)))
        }
      case Success(false) =>
        -\/(ValueFailure(path, NullValue))
      case Failure(_) =>
        -\/(ValueFailure(path, MissingValue))
    }
  }

  def validateConfig[State, ValidConfig](
    configFile: String
  )(check: Config => ConfigError \/ ValidConfig
  ): ConfigError \/ ValidConfig = {
    check(ConfigFactory.load(configFile))
  }
}
