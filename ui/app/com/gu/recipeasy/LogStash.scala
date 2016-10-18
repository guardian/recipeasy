package com.gu.recipeasy

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{ Logger, LoggerContext }
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.regions.Region
import com.gu.cm.Identity
import com.gu.logback.appender.kinesis.KinesisAppender
import net.logstash.logback.layout.LogstashLayout
import org.slf4j.{ LoggerFactory, Logger => SLFLogger }
import play.api.Mode.Mode

import scala.util.control.NonFatal

case class KinesisAppenderConfig(
  stream: String,
  credentialsProvider: AWSCredentialsProviderChain,
  region: Region,
  bufferSize: Int = 1000
)

object LogStash {
  val logger = LoggerFactory.getLogger("LogStash")

  // assume SLF4J is bound to logback in the current environment
  lazy val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  def makeCustomFields(customFields: Map[String, String]): String = {
    "{" + (for ((k, v) <- customFields) yield (s""""${k}":"${v}"""")).mkString(",") + "}"
  }

  def makeLayout(customFields: String) = {
    val l = new LogstashLayout()
    l.setCustomFields(customFields)
    l
  }

  def makeKinesisAppender(layout: LogstashLayout, context: LoggerContext, appenderConfig: KinesisAppenderConfig) = {
    val a = new KinesisAppender[ILoggingEvent]()
    a.setStreamName(appenderConfig.stream)
    a.setRegion(appenderConfig.region.getName)
    a.setCredentialsProvider(appenderConfig.credentialsProvider)
    a.setBufferSize(appenderConfig.bufferSize)

    a.setContext(context)
    a.setLayout(layout)

    layout.start()
    a.start()
    a
  }

  def init(config: KinesisAppenderConfig, mode: Mode, identity: Identity) {

    val FACTS: Map[String, String] = try {
      val facts: Map[String, String] = {
        logger.info("Loading facts from AWS instance tags")
        Map("app" -> identity.app, "stack" -> identity.stack, "stage" -> identity.stage)
      }
      logger.info(s"Using facts: $facts")
      facts
    } catch {
      case NonFatal(e) =>
        logger.error("Failed to get facts", e)
        Map.empty
    }

    try {
      logger.info("Configuring logging to send to LogStash")
      val layout = makeLayout(makeCustomFields(FACTS))
      val appender = makeKinesisAppender(layout, context, config)
      val rootLogger = LoggerFactory.getLogger(SLFLogger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
      rootLogger.addAppender(appender)
      logger.info("LogStash configuration completed")
    } catch {
      case e: JoranException => // ignore, errors will be printed below
      case NonFatal(e) =>
        logger.error("Error while initialising LogStash", e)
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(context)
  }
}
