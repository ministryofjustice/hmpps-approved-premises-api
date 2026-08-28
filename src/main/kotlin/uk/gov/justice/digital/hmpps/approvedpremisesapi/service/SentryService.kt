package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SentryService {
  fun captureException(throwable: Throwable)

  /**
   * @param message The message
   * @param groupId Used by sentry to group messages together. This should be defined where the message content varies but you want them grouping together
   */
  fun captureErrorMessage(message: String, groupId: String? = null)
}

@Service
class SentryServiceImpl : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun captureException(throwable: Throwable) {
    log.info("Will capture exception in sentry", throwable)
    Sentry.captureException(throwable)
  }

  override fun captureErrorMessage(
    message: String,
    groupId: String?,
  ) {
    log.info("Will capture error message in sentry: '$message' with group id '$groupId'")

    Sentry.withScope { scope ->
      if (groupId != null) {
        scope.fingerprint = listOf(groupId)
      }
      Sentry.captureMessage(message)
    }
  }
}
