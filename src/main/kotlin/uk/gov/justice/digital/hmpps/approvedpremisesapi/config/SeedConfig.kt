package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "seed")
class SeedConfig {
  lateinit var filePrefix: String
}
