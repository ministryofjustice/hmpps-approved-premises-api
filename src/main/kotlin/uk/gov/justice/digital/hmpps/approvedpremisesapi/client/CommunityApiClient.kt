package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

@Component
class CommunityApiClient(
  restTemplate: RestTemplate,
  objectMapper: ObjectMapper,
  hmppsAuthClient: HMPPSAuthClient,
  @Value("\${services.community-api.base-url}") communityApiBaseUrl: String
) : BaseHMPPSClient(restTemplate, objectMapper, hmppsAuthClient, communityApiBaseUrl) {
  fun getOffenderDetailSummary(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
    authType = HMPPSAuthType.ClientCredentials
  }

  fun getUserAccessForOffenderCrn(userDistinguishedName: String, crn: String) = getRequest<UserOffenderAccess> {
    path = "/secure/offenders/crn/$crn/user/$userDistinguishedName/userAccess"
    authType = HMPPSAuthType.ClientCredentials
  }
}
