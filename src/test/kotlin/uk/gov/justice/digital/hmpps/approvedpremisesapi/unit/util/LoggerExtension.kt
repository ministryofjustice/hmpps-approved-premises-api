package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory

class LoggerExtension :
  BeforeEachCallback,
  AfterEachCallback {

  private val listAppender = ListAppender<ILoggingEvent>()
  private val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

  override fun beforeEach(context: ExtensionContext?) {
    logger.addAppender(listAppender)
    listAppender.start()
  }

  override fun afterEach(context: ExtensionContext?) {
    listAppender.stop()
    listAppender.list.clear()
    logger.detachAppender(listAppender)
  }

  fun assertNoLogs() {
    org.assertj.core.api.Assertions.assertThat(listAppender.list).isEmpty()
  }

  fun assertContains(message: String) {
    val messages = listAppender.list.map { it.message }
    org.assertj.core.api.Assertions.assertThat(messages).contains(message)
  }

  fun assertError(message: String, cause: Throwable) {
    assertContains(message)
    val log = listAppender.list.first { it.message == message }
    org.assertj.core.api.Assertions.assertThat((log.throwableProxy as ThrowableProxy?)?.throwable).isEqualTo(cause)
  }
}
