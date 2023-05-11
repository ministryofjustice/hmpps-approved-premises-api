package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SeedLogger {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun info(message: String) = log.info(message)
  fun warn(message: String) = log.warn(message)
  fun error(message: String) = log.error(message)
  fun error(message: String, throwable: Throwable) = log.error(message, throwable)
}
