package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Service
@Primary
class NoOpSentryService : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  private val capturedExceptions = mutableListOf<Throwable>()
  private val capturedErrors = mutableListOf<String>()

  override fun captureException(throwable: Throwable) {
    log.info("Sentry Exception Captured", throwable)
    capturedExceptions.add(throwable)
  }

  override fun captureErrorMessage(message: String) {
    log.info("Sentry Message Captured : '$message'", message)
    capturedErrors.add(message)
  }

  fun getRaisedExceptions() = capturedExceptions

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedExceptions.clear()
    capturedErrors.clear()
  }
}
