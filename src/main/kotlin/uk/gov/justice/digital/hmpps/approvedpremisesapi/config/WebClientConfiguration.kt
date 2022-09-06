package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {
  @Bean(name = ["communityApiWebClient"])
  fun communityApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.community-api.base-url}") communityApiBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("community-api")

    return WebClient.builder()
      .baseUrl(communityApiBaseUrl)
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["assessRisksAndNeedsApiWebClient"])
  fun assessRisksAndNeedsApiWebClient(
    @Value("\${services.assess-risks-and-needs-api.base-url}") assessRisksAndNeedsApiBaseUrl: String
  ): WebClient {
    return WebClient.builder()
      .baseUrl(assessRisksAndNeedsApiBaseUrl)
      .build()
  }
}
