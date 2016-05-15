package cakesolutions.logging

import org.slf4j.{LoggerFactory, MDC}

object ContextualLogging {
  case class LoggingKey(key: String)

  case class Context(kvs: (LoggingKey, Any)*)
}

trait ContextualLogging {
  import ContextualLogging._

  private type KV = (LoggingKey, Any)
  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  protected object log {
    def debug(message: String)(kvs: KV*)(implicit context: Context = Context()): Unit = {
      withContext(kvs: _*) {
        logger.debug(message)
      }
    }

    def info(message: String)(kvs: KV*)(implicit context: Context = Context()): Unit = {
      withContext(kvs: _*) {
        logger.info(message)
      }
    }

    def warn(message: String)(kvs: KV*)(implicit context: Context = Context()): Unit = {
      withContext(kvs: _*) {
        logger.warn(message)
      }
    }

    def error(message: String, exn: Throwable = null)(kvs: KV*)(implicit context: Context = Context()): Unit = {
      withContext(kvs: _*) {
        if (exn == null) {
          logger.error(message)
        } else {
          logger.error(message, exn)
        }
      }
    }
  }

  def withContext[T](kvs: KV*)(block: => T)(implicit context: Context): T = {
    (context.kvs ++ kvs).foreach {
      case (k, v) =>
        MDC.put(k.key, v.toString)
    }
    val result = block
    (context.kvs ++ kvs).foreach {
      case (k, v) =>
        MDC.remove(k.key)
    }
    result
  }
}
