package cakesolutions.application

import java.util.concurrent.ExecutorService

import cakesolutions.logging.ContextualLogging
import cakesolutions.logging.ContextualLogging.{Context, LoggingKey}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}

object ApplicationFSM {
  val applicationKey = LoggingKey("application")
  val environmentKey = LoggingKey("environment")
  val stateKey = LoggingKey("state")
  val statusKey = LoggingKey("status")

  trait ApplicationFSMError
  case class InternalError(cause: Throwable) extends ApplicationFSMError
  case class UndefinedTransition[State](state: State) extends ApplicationFSMError
  case object RetryAttemptsExhausted extends ApplicationFSMError

  case class RetryAttempt[State](retries: Int, timeout: FiniteDuration, readyCheck: State => Future[Boolean]) {
    def run(attemptsLeft: Int = retries): ApplicationFSMError \/ State = {
//      if (attemptsLeft <= 0) {
//        -\/(RetryAttemptsExhausted)
//      } else {
//        readyCheck(???).onComplete {
//          case Success(true) =>
//            \/-(???)
//
//          case Success(false) =>
//            retry(attemptsLeft - 1)
//
//          case Failure(exn) =>
//            retry(attemptsLeft - 1)
//        }
//      }
      ???
    }

    def abort(): ApplicationFSMError \/ State = ???
  }
}

trait ApplicationFSM extends App with ContextualLogging {
  import ApplicationFSM._

  type State
  protected implicit def context: Context
  protected def initial: State
  protected def isFinal(state: State): Boolean
  protected def threadPool: ExecutorService
  protected def transition()(implicit context: Context): PartialFunction[State, Future[ApplicationFSMError \/ State]]

  private[this] lazy val ec = ExecutionContext.fromExecutorService(threadPool)

  run()(ec, context).onComplete {
    case Success(\/-(state)) =>
      log.info("Application successful")(statusKey -> state)
      sys.exit(0)

    case Success(-\/(failure)) =>
      log.error("Application failed")(statusKey -> failure)
      sys.exit(1)

    case Failure(exn) =>
      log.error("Application exception", exn)(statusKey -> exn)
      sys.exit(2)
  } (ec)

  def syncReturn[State](
    action: => ApplicationFSMError \/ State
  )(implicit context: Context
  ): Future[ApplicationFSMError \/ State] = {
    Future(action)(ec)
  }

  def asyncReturn[State, Result](
    action: Promise[ApplicationFSMError \/ State] => Result
  )(implicit context: Context
  ): Future[ApplicationFSMError \/ State] = {
    val callback = Promise[ApplicationFSMError \/ State]
    Future(action(callback))(ec).onFailure {
      case NonFatal(exn) =>
        log.error("Async action failed", exn)()
        callback.failure(exn)
    } (ec)
    callback.future
  }

  private def run(
    state: State = initial
  )(implicit ec: ExecutionContext,
    context: Context
  ): Future[ApplicationFSMError \/ State] = {
    if (isFinal(state)) {
      Future.successful(\/-(state))
    } else if (transition().isDefinedAt(state)) {
      transition().apply(state).flatMap {
        case \/-(nextState) =>
          run(nextState)

        case error =>
          Future.successful(error)
      }
    } else {
      Future.successful(-\/(UndefinedTransition[State](state)))
    }
  }
}