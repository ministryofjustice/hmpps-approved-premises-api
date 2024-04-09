package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import io.sentry.SentryLevel
import org.springframework.stereotype.Service

@Service
class SentryService {
  fun captureException(throwable: Throwable) {
    Sentry.captureException(throwable)
  }

  fun captureErrorMessage(message: String) {
    Sentry.captureMessage(message, SentryLevel.ERROR)
  }
}
