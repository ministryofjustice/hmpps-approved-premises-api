package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.WithdrawnBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails

@Component
class DomainEventTransformer(private val communityApiClient: CommunityApiClient) {
  fun toWithdrawnBy(user: UserEntity) =
    when (val result = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> toWithdrawnBy(result.body)
      is ClientResult.Failure -> result.throwException()
    }

  fun toWithdrawnBy(staffDetails: StaffUserDetails): WithdrawnBy {
    val staffMember = staffDetails.toStaffMember()
    val probationArea = toProbationArea(staffDetails)
    return WithdrawnBy(staffMember, probationArea)
  }

  fun toProbationArea(staffDetails: StaffUserDetails): ProbationArea {
    return ProbationArea(
      code = staffDetails.probationArea.code,
      name = staffDetails.probationArea.description,
    )
  }

  fun toStaffMember(user: UserEntity) =
    when (val result = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> result.body.toStaffMember()
      is ClientResult.Failure -> result.throwException()
    }

  fun toStaffMember(staffDetails: StaffUserDetails): StaffMember = staffDetails.toStaffMember()
}
