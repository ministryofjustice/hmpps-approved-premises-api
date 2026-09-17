package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.hmpps.kotlin.auth.ServletRequestResponseNonNullFilterFunction
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
) {

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

  @Bean(name = ["cas1UiWebClient"])
  fun cas1UiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.cas1-ui.base-url}") cas1UiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(cas1UiBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.ap-delius-context-api.base-url}") apDeliusContextApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(apDeliusContextApiBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String,
    @Value("\${services.hmpps-tier.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("hmpps-tier")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(hmppsTierApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(tierApiUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(tierApiUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisons-api")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(prisonsApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(defaultUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(defaultUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["prisonerAlertsApiWebClient"])
  fun prisonerAlertsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.prisoner-alerts-api.base-url}") prisonerAlertsApiBaseUrl: String,
    @Value("\${services.prisoner-alerts-api.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisoner-alerts-api")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(prisonerAlertsApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(tierApiUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(tierApiUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    authorizedClients: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String,
    @Value("\${services.case-notes.timeout-ms}") caseNotesServiceUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("case-notes")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(caseNotesBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String,
    @Value("\${services.ap-oasys-context-api.timeout-ms}") apAndOasysUpstreamTimeoutMs: Long,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("ap-oasys-context")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(apOASysContextApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(apAndOasysUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(apAndOasysUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(ServletRequestResponseNonNullFilterFunction())
        .filter(oauth2Client)
        .build(),
      retryOnReadTimeout = true,
    )
  }

  @Bean(name = ["govUKBankHolidaysApiWebClient"])
  fun govUKBankHolidaysApiClient(
    @Value("\${services.gov-uk-bank-holidays-api.base-url}") govUKBankHolidaysApiBaseUrl: String,
    webClientBuilder: WebClient.Builder,
  ): WebClientConfig = WebClientConfig(
    webClientBuilder
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
    webClientBuilder: WebClient.Builder,
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
    @Value("\${services.nomis-user-roles-api.timeout-ms}") nomisUserRolesUpstreamTimeoutMs: Long,
  ): WebClientConfig = WebClientConfig(
    webClientBuilder
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
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("nomis-user-roles-api")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(nomisUserRolesBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }

  @Bean(name = ["manageUsersApiWebClient"])
  fun manageUsersApiClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.manage-users-api.base-url}") manageUsersBaseUrl: String,
  ): WebClientConfig = WebClientConfig(
    webClientBuilder
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
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("manage-pom-cases")

    return WebClientConfig(
      webClientBuilder
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }

  @Bean(name = ["nonAssociationsWebClient"])
  fun nonAssociationsWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("non-associations")

    return WebClientConfig(
      webClientBuilder
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }

  @Bean(name = ["prisonerSearchWebClient"])
  fun prisonerSearchWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisoner-search.base-url}") prisonSearchBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisoner-search")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(prisonSearchBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }

  @Bean(name = ["licenceApiWebClient"])
  fun licenceApiWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.licence-api.base-url}") licenceApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("cvl")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(licenceApiBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }

  @Bean(name = ["healthAndMedicationApiWebClient"])
  fun healthAndMedicationApiWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.health-and-medication-api.base-url}") healthAndMedicationApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("health-and-medication")

    return WebClientConfig(
      webClientBuilder
        .baseUrl(healthAndMedicationApiBaseUrl)
        .filter(ServletRequestResponseNonNullFilterFunction())
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
        .build(),
    )
  }
}
