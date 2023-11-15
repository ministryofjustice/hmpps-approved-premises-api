package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail

@Component
class NomisUserRolesApiClient(
  @Qualifier("nomisUserRolesApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {

  fun getUserDetails(jwt: String) = getRequest<NomisUserDetail> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/me"
  }
}
