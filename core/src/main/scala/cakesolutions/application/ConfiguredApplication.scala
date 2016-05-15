package cakesolutions.application

import cakesolutions.application.ConfigUtils.ConfigError
import cakesolutions.logging.ContextualLogging.{Context, LoggingKey}

import scala.concurrent.Promise
import scalaz.\/

object ConfiguredApplication {
  val configurationKey = LoggingKey("configuration")

  sealed trait State
  case object LoadConfig extends State
  final case class Running[ValidConfig](config: ValidConfig) extends State
  final case class Result[Value](value: Value) extends State
}

trait ConfiguredApplication[ValidConfig] extends ApplicationFSM {
  import ApplicationFSM._
  import ConfiguredApplication.{State => ApplicationState, _}

  type State = ApplicationState

  protected def validatedConfig: ConfigError \/ ValidConfig

  protected def workload(config: ValidConfig)(implicit callback: Promise[ApplicationFSMError \/ State]): Unit

  protected override lazy val initial = LoadConfig

  final protected override def isFinal(state: State): Boolean = state.isInstanceOf[Result[_]]

  protected override def transition()(implicit context: Context) = {
    case state @ LoadConfig =>
      syncReturn {
        withContext(stateKey -> state) {
          log.info("Application started")(environmentKey -> sys.env)
          validatedConfig.map { checkedConfig =>
            log.info("Configuration loaded")(configurationKey -> checkedConfig)
            Running[ValidConfig](checkedConfig)
          }
        }
      }

    case state: Running[ValidConfig] =>
      asyncReturn[State, Unit] { implicit callback =>
        withContext(stateKey -> state) {
          log.info("Workload started")()
          workload(state.config)
        }
      }
  }
}
