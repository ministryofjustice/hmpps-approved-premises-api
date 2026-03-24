package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class NomisUserRolesForRequesterApiClient(
  @Qualifier("nomisUserRolesForRequesterApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  fun getUserDetailsForMe(jwt: String) = getRequest<NomisUserDetail> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/me"
  }
}
