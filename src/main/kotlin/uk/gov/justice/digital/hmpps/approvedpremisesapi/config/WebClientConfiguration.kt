package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.unauthenticatedWebClient
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
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "delius-backed-apis",
        url = cas1UiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.ap-delius-context-api.base-url}") apDeliusContextApiBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "delius-backed-apis",
        url = apDeliusContextApiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String,
    @Value("\${services.hmpps-tier.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "hmpps-tier",
        url = hmppsTierApiBaseUrl,
        timeout = Duration.ofMillis(tierApiUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(tierApiUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "prisons-api",
        url = prisonsApiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["prisonerAlertsApiWebClient"])
  fun prisonerAlertsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.prisoner-alerts-api.base-url}") prisonerAlertsApiBaseUrl: String,
    @Value("\${services.prisoner-alerts-api.timeout-ms}") tierApiUpstreamTimeoutMs: Long,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "prisoner-alerts-api",
        url = prisonerAlertsApiBaseUrl,
        timeout = Duration.ofMillis(tierApiUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(tierApiUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    authorizedClients: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String,
    @Value("\${services.case-notes.timeout-ms}") caseNotesServiceUpstreamTimeoutMs: Long,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClients,
        registrationId = "case-notes",
        url = caseNotesBaseUrl,
        timeout = Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    webClientBuilder: WebClient.Builder,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String,
    @Value("\${services.ap-oasys-context-api.timeout-ms}") apAndOasysUpstreamTimeoutMs: Long,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "ap-oasys-context",
        url = apOASysContextApiBaseUrl,
        timeout = Duration.ofMillis(apAndOasysUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(apAndOasysUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["govUKBankHolidaysApiWebClient"])
  fun govUKBankHolidaysApiClient(
    @Value("\${services.gov-uk-bank-holidays-api.base-url}") govUKBankHolidaysApiBaseUrl: String,
    webClientBuilder: WebClient.Builder,
  ) = WebClientConfig(
    webClientBuilder
      .unauthenticatedWebClient(
        url = govUKBankHolidaysApiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["nomisUserRolesForRequesterApiWebClient"])
  fun nomisUserRolesForRequesterApiClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
    @Value("\${services.nomis-user-roles-api.timeout-ms}") nomisUserRolesUpstreamTimeoutMs: Long,
  ) = WebClientConfig(
    webClientBuilder
      .unauthenticatedWebClient(
        url = nomisUserRolesBaseUrl,
        timeout = Duration.ofMillis(nomisUserRolesUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(nomisUserRolesUpstreamTimeoutMs),
      ),
    retryOnReadTimeout = true,
  )

  @Bean(name = ["nomisUserRolesApiWebClient"])
  fun nomisUserRolesApiClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "nomis-user-roles-api",
        url = nomisUserRolesBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["manageUsersApiWebClient"])
  fun manageUsersApiClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.manage-users-api.base-url}") manageUsersBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .unauthenticatedWebClient(
        url = manageUsersBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["managePomCasesWebClient"])
  fun managePomCasesWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "manage-pom-cases",
        url = "",
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["nonAssociationsWebClient"])
  fun nonAssociationsWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "non-associations",
        url = "",
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["prisonerSearchWebClient"])
  fun prisonerSearchWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisoner-search.base-url}") prisonSearchBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "prisoner-search",
        url = prisonSearchBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["licenceApiWebClient"])
  fun licenceApiWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.licence-api.base-url}") licenceApiBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "cvl",
        url = licenceApiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )

  @Bean(name = ["healthAndMedicationApiWebClient"])
  fun healthAndMedicationApiWebClient(
    webClientBuilder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.health-and-medication-api.base-url}") healthAndMedicationApiBaseUrl: String,
  ) = WebClientConfig(
    webClientBuilder
      .authorisedWebClient(
        authorizedClientManager = authorizedClientManager,
        registrationId = "health-and-medication",
        url = healthAndMedicationApiBaseUrl,
        timeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
        connectionTimeout = Duration.ofMillis(defaultUpstreamTimeoutMs),
      ),
  )
}
