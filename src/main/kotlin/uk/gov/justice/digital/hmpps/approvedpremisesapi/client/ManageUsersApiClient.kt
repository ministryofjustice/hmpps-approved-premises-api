package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageusers.ExternalUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ManageUsersApiClient(
  @Qualifier("manageUsersApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  fun getExternalUserDetails(username: String, jwt: String) = getRequest<ExternalUserDetails> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/externalusers/$username"
  }
}
