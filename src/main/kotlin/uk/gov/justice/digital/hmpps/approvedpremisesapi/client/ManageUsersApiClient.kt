package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.manageusers.ExternalUserDetails

@Component
class ManageUsersApiClient(
  @Qualifier("manageUsersApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {

  fun getExternalUserDetails(username: String, jwt: String) = getRequest<ExternalUserDetails> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/externalusers/$username"
  }
}
