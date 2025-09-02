package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
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
    val service: OAuth2AuthorizedClientService = ClientCachingOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
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
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["prisonerAlertsApiWebClient"])
  fun prisonerAlertsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisoner-alerts-api.base-url}") prisonerAlertsApiBaseUrl: String,
    @Value("\${services.prisoner-alerts-api.max-response-in-memory-size-bytes}") prisonerAlertsApiMaxResponseInMemorySizeBytes: Int,
    @Value("\${services.prisoner-alerts-api.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
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
              .responseTimeout(Duration.ofMillis(tierApiUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(tierApiUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(prisonerAlertsApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
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
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
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

  @Bean(name = ["nomisUserRolesForRequesterApiWebClient"])
  fun nomisUserRolesForRequesterApiClient(
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

  @Bean(name = ["nomisUserRolesApiWebClient"])
  fun nomisUserRolesApiClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("nomis-user-roles-api")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(nomisUserRolesBaseUrl)
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

  @Bean(name = ["managePomCasesWebClient"])
  fun managePomCasesWebClient(
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
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisoner-search.base-url}") prisonSearchBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisoner-search")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(prisonSearchBaseUrl)
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
