package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MigrationLogger {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun info(message: String) = log.info(message)
  fun error(message: String) = log.error(message)
  fun error(message: String, throwable: Throwable) = log.error(message, throwable)
}
