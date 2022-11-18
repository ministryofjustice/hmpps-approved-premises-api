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

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClient.builder()
      .baseUrl(communityApiBaseUrl)
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.ap-delius-context-api.base-url}") communityApiBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

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

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("hmpps-tier")

    return WebClient.builder()
      .baseUrl(hmppsTierApiBaseUrl)
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("prisons-api")

    return WebClient.builder()
      .baseUrl(prisonsApiBaseUrl)
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("case-notes")

    return WebClient.builder()
      .baseUrl(caseNotesBaseUrl)
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("ap-oasys-context")

    return WebClient.builder()
      .baseUrl(apOASysContextApiBaseUrl)
      .filter(oauth2Client)
      .build()
  }
}
