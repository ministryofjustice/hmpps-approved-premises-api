package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.micrometer.azuremonitor.AzureMonitorConfig
import io.micrometer.azuremonitor.AzureMonitorMeterRegistry
import io.micrometer.core.instrument.Clock.SYSTEM
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationInsightsConfiguration {

  @Bean
  @ConditionalOnProperty("applicationinsights.connection.string")
  fun azureMonitorMeterRegistry(@Value($$"${applicationinsights.connection.string}") connectionString: String): AzureMonitorMeterRegistry = AzureMonitorMeterRegistry(
    object : AzureMonitorConfig {
      override fun get(key: String): String? = null
      override fun connectionString(): String = connectionString
    },
    SYSTEM,
  )
}
