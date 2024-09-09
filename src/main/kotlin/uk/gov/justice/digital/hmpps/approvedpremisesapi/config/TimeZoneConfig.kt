package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.annotation.PostConstruct
import kotlinx.datetime.TimeZone
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TimeZoneConfig {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logTimeZone() {
    log.info("JDK Time Zone is ${TimeZone.currentSystemDefault()}")
  }
}
