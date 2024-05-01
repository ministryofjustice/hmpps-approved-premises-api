package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.flipt.api.FliptClient
import io.flipt.api.authentication.ClientTokenAuthenticationStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FliptConfig(
  @Value("\${flipt.enabled}") private val enabled: Boolean,
  @Value("\${flipt.url:#{null}}") private val url: String?,
  @Value("\${flipt.token:#{null}}") private val token: String?,
) {
  @Bean
  fun fliptApiClient(): FliptClient? = if (enabled) {
    FliptClient.builder().url(url!!).authentication(ClientTokenAuthenticationStrategy(token!!)).build()
  } else {
    null
  }
}
