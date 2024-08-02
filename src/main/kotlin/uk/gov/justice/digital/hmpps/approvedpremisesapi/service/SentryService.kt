package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import io.sentry.SentryLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SentryService {
  fun captureException(throwable: Throwable)
  fun captureErrorMessage(message: String)
}

@Service
class SentryServiceImpl : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun captureException(throwable: Throwable) {
    log.debug("Will capture exception in sentry", throwable)
    Sentry.captureException(throwable)
  }

  override fun captureErrorMessage(message: String) {
    log.debug("Will capture error message in sentry: '$message'")
    Sentry.captureMessage(message, SentryLevel.ERROR)
  }
}
