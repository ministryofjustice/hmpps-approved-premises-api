package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

@Component
class CommunityApiClient(
  @Qualifier("communityApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  @Cacheable(value = ["offenderDetailsCache"], unless = IS_NOT_SUCCESSFUL)
  fun getOffenderDetailSummary(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
  }

  @Cacheable(value = ["userAccessCache"], unless = IS_NOT_SUCCESSFUL)
  fun getUserAccessForOffenderCrn(userDistinguishedName: String, crn: String) = getRequest<UserOffenderAccess> {
    path = "/secure/offenders/crn/$crn/user/$userDistinguishedName/userAccess"
  }

  fun getRegistrationsForOffenderCrn(crn: String) = getRequest<Registrations> {
    path = "/secure/offenders/crn/$crn/registrations?activeOnly=true"
  }

  fun getStaffUserDetails(deliusUsername: String) = getRequest<StaffUserDetails> {
    path = "/secure/staff/username/$deliusUsername"
  }

  fun getConvictions(crn: String) = getRequest<List<Conviction>> {
    path = "/secure/offenders/crn/$crn/convictions"
  }

  fun getDocuments(crn: String) = getRequest<GroupedDocuments> {
    path = "/secure/offenders/crn/$crn/documents/grouped"
  }
}
