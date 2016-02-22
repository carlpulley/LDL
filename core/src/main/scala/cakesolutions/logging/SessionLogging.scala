package cakesolutions.logging

import akka.actor.Actor
import akka.event.{DiagnosticLoggingAdapter, Logging}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

final case class KeyMissingException(keys: Set[String], required: Set[String]) extends Exception

final case class SessionContext(keys: Map[String, Any])(implicit config: Config) {
  private val requiredKeys: Set[String] = config.getStringList("logging.keys.required").asScala.toSet

  if (keys.keySet.intersect(requiredKeys) != requiredKeys) {
    throw KeyMissingException(keys.keySet, requiredKeys)
  }
}

trait SessionLogging extends Actor {

  val log: DiagnosticLoggingAdapter = Logging(this)

  def withContext(loggingBlock: => Unit)(implicit context: SessionContext): Unit = {
    try {
      log.setMDC(context.keys.asJava)
      loggingBlock
    } finally {
      log.clearMDC()
    }
  }

}
