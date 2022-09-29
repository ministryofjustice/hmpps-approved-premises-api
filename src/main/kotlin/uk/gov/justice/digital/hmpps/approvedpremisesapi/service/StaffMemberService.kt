package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.shouldNotBeReached

@Service
class StaffMemberService(private val communityApiClient: CommunityApiClient) {
  fun getStaffMemberById(id: Long): AuthorisableActionResult<StaffMember> = when (val staffMemberResponse = communityApiClient.getStaffMember(id)) {
    is ClientResult.Success -> AuthorisableActionResult.Success(staffMemberResponse.body)
    is ClientResult.StatusCodeFailure -> if (staffMemberResponse.status == HttpStatus.NOT_FOUND) AuthorisableActionResult.NotFound() else staffMemberResponse.throwException()
    is ClientResult.Failure -> staffMemberResponse.throwException()
    else -> shouldNotBeReached()
  }
}
