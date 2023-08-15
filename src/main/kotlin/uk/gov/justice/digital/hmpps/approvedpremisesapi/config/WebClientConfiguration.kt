package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class WebClientConfiguration(@Value("\${upstream-timeout-ms}") private val upstreamTimeoutMs: Long) {
  @Bean
  fun authorizedClientManager(clients: ClientRegistrationRepository): OAuth2AuthorizedClientManager {
    val service: OAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }

  @Bean(name = ["communityApiWebClient"])
  fun communityApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.community-api.base-url}") communityApiBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClient.builder()
      .baseUrl(communityApiBaseUrl)
      .filter(oauth2Client)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build()
  }

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-delius-context-api.base-url}") communityApiBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClient.builder()
      .baseUrl(communityApiBaseUrl)
      .filter(oauth2Client)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build()
  }

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("hmpps-tier")

    return WebClient.builder()
      .baseUrl(hmppsTierApiBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisons-api")

    return WebClient.builder()
      .baseUrl(prisonsApiBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("case-notes")

    return WebClient.builder()
      .baseUrl(caseNotesBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("ap-oasys-context")

    return WebClient.builder()
      .baseUrl(apOASysContextApiBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .filter(oauth2Client)
      .build()
  }

  @Bean(name = ["govUKBankHolidaysApiWebClient"])
  fun govUKBankHolidaysApiClient(
    @Value("\${services.gov-uk-bank-holidays-api.base-url}") govUKBankHolidaysApiBaseUrl: String,
  ): WebClient {
    return WebClient.builder()
      .baseUrl(govUKBankHolidaysApiBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build()
  }
}
