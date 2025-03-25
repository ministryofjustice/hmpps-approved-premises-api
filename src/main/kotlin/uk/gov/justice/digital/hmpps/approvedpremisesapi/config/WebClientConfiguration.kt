package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
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
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

data class WebClientConfig(
  val webClient: WebClient,
  val maxRetryAttempts: Long = 1,
  val retryOnReadTimeout: Boolean = false,
)

@SuppressWarnings("LongParameterList")
@Configuration
class WebClientConfiguration(
  @Value("\${services.default.timeout-ms}") private val defaultUpstreamTimeoutMs: Long,
  @Value("\${services.default.max-response-in-memory-size-bytes}") private val defaultMaxResponseInMemorySizeBytes: Int,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

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

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-delius-context-api.base-url}") apDeliusContextApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(apDeliusContextApiBaseUrl)
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(defaultMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String,
    @Value("\${services.hmpps-tier.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("hmpps-tier")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(hmppsTierApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(tierApiUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(tierApiUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String,
    @Value("\${services.prisons-api.max-response-in-memory-size-bytes}") prisonApiMaxResponseInMemorySizeBytes: Int,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisons-api")

    log.info("Using maxInMemorySize of $prisonApiMaxResponseInMemorySizeBytes bytes for Prison API Web Client")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(prisonsApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(prisonApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["prisonerAlertsApiWebClient"])
  fun prisonerAlertsApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisoner-alerts-api.base-url}") prisonerAlertsApiBaseUrl: String,
    @Value("\${services.prisoner-alerts-api.max-response-in-memory-size-bytes}") prisonerAlertsApiMaxResponseInMemorySizeBytes: Int,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisoner-alerts-api")

    log.info("Using maxInMemorySize of $prisonerAlertsApiMaxResponseInMemorySizeBytes bytes for Prisoner Alerts API Web Client")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(prisonerAlertsApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(prisonerAlertsApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String,
    @Value("\${services.case-notes.timeout-ms}") caseNotesServiceUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("case-notes")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(caseNotesBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String,
    @Value("\${services.ap-oasys-context-api.timeout-ms}") apAndOasysUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("ap-oasys-context")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(apOASysContextApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(apAndOasysUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(apAndOasysUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["govUKBankHolidaysApiWebClient"])
  fun govUKBankHolidaysApiClient(
    @Value("\${services.gov-uk-bank-holidays-api.base-url}") govUKBankHolidaysApiBaseUrl: String,
  ): WebClientConfig = WebClientConfig(
    WebClient.builder()
      .baseUrl(govUKBankHolidaysApiBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build(),
  )

  @Bean(name = ["nomisUserRolesApiWebClient"])
  fun nomisUserRolesApiClient(
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
    @Value("\${services.nomis-user-roles-api.timeout-ms}") nomisUserRolesUpstreamTimeoutMs: Long,
  ): WebClientConfig = WebClientConfig(
    WebClient.builder()
      .baseUrl(nomisUserRolesBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(nomisUserRolesUpstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(nomisUserRolesUpstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build(),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["manageUsersApiWebClient"])
  fun manageUsersApiClient(
    @Value("\${services.manage-users-api.base-url}") manageUsersBaseUrl: String,
  ): WebClientConfig = WebClientConfig(
    WebClient.builder()
      .baseUrl(manageUsersBaseUrl)
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient
            .create()
            .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
        ),
      )
      .build(),
  )

  @Bean(name = ["probationOffenderSearchApiWebClient"])
  fun probationOffenderSearchApiClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.probation-offender-search-api.base-url}") probationOffenderSearchBaseUrl: String,
    @Value("\${services.probation-offender-search-api.max-response-in-memory-size-bytes}") probationOffenderSearchApiMaxResponseInMemorySizeBytes: Int,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      webClient = WebClient.builder()
        .baseUrl(probationOffenderSearchBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(probationOffenderSearchApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
    )
  }

  @Bean(name = ["managePomCasesWebClient"])
  fun managePomCasesWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("manage-pom-cases")

    return WebClientConfig(
      WebClient.builder()
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt(),
              ),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(defaultMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
    )
  }

  @Bean(name = ["prisonerSearchWebClient"])
  fun prisonerSearchWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisoner-search")

    return WebClientConfig(
      WebClient.builder()
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt(),
              ),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(defaultMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
    )
  }
}
