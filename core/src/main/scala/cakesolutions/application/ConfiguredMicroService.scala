package cakesolutions.application

import cakesolutions.application.ConfigUtils.ConfigError
import cakesolutions.logging.ContextualLogging.Context
import shapeless.HList

import scala.concurrent.{Future, Promise}
import scalaz.\/

object ConfiguredMicroService {
  sealed trait State
  case class Result[Value](value: Value) extends State
  case class Running[ValidConfig](config: ValidConfig, services: HList) extends State
  case class ServiceWait[ValidConfig](config: ValidConfig) extends State
  case object LoadConfig extends State
}

trait ConfiguredMicroService[ValidConfig] extends ApplicationFSM {
  import ApplicationFSM._
  import ConfiguredApplication.configurationKey
  import ConfiguredMicroService.{State => ApplicationState, _}

  override type State = ApplicationState

  // TODO: deal with multiple service waits!!!
  protected def serviceWait(
    config: ValidConfig
  )(readyCheck: RetryAttempt[State]
  )(implicit callback: Promise[ApplicationFSMError \/ State]
  ): Unit = {
    ???
  }

  protected def validatedConfig: ConfigError \/ ValidConfig

  protected def workload(config: ValidConfig, services: HList)(implicit callback: Promise[ApplicationFSMError \/ State]): Unit

  protected override lazy val initial = LoadConfig

  // Micro-service applications are long living, and so recognise no final states
  final protected override def isFinal(state: State): Boolean = false

  protected override def transition()(implicit context: Context) = {
    case state @ LoadConfig =>
      syncReturn {
        withContext(stateKey -> state) {
          log.info("Application started")(environmentKey -> sys.env)
          validatedConfig.map { checkedConfig =>
            log.info("Configuration loaded")(configurationKey -> checkedConfig)
            ServiceWait[ValidConfig](checkedConfig)
          }
        }
      }

    case state: ServiceWait[ValidConfig] =>
      asyncReturn[State, Unit] { implicit callback =>
        withContext(stateKey -> state) {
          // TODO: service checking!
          log.info("All services are ready")()
          Running[ValidConfig](state.config, ???) // TODO: add service instances to Running state
        }
      }

    case state: Running[ValidConfig] =>
      asyncReturn[State, Unit] { implicit callback =>
        withContext(stateKey -> state) {
          log.info("Workload started")()
          workload(state.config, state.services)
        }
      }
  }
}
