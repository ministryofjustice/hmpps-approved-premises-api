package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfiguration {
  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
    return builder
      .errorHandler(NoopResponseErrorHandler())
      .build()
  }
}

class NoopResponseErrorHandler : ResponseErrorHandler {
  override fun hasError(response: ClientHttpResponse): Boolean {
    return false
  }

  override fun handleError(response: ClientHttpResponse) = Unit
}
