package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class NomisUserRolesApiClient(
  @Qualifier("nomisUserRolesApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun getUserDetails(username: String) = getRequest<NomisUserDetail> {
    path = "/users/$username"
  }

  fun getUserStaffInformation(staffId: Long) = getRequest<NomisStaffInformation> {
    path = "/users/staff/$staffId"
  }
}
