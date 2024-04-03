package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import org.springframework.stereotype.Service

@Service
class SentryService {
  fun captureException(throwable: Throwable) {
    Sentry.captureException(throwable)
  }
}
