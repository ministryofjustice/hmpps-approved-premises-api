package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageusers.ExternalUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ManageUsersApiClient(
  @Qualifier("manageUsersApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun getExternalUserDetails(username: String, jwt: String) = getRequest<ExternalUserDetails> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/externalusers/$username"
  }
}
