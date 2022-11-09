package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

@Service
class StaffMemberService(private val apDeliusContextApiClient: ApDeliusContextApiClient) {
  fun getStaffMemberByCode(code: String, qCode: String): AuthorisableActionResult<StaffMember> {
    val premisesStaffMembersResult = getStaffMembersForQCode(qCode)

    val premisesStaffMembers = when (premisesStaffMembersResult) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> premisesStaffMembersResult.entity
    }

    val staffMember = premisesStaffMembers.content.firstOrNull { it.code == code }
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(staffMember)
  }

  fun getStaffMembersForQCode(qCode: String) = when (val staffMembersResponse = apDeliusContextApiClient.getStaffMembers(qCode)) {
    is ClientResult.Success -> AuthorisableActionResult.Success(staffMembersResponse.body)
    is ClientResult.Failure.StatusCode -> if (staffMembersResponse.status == HttpStatus.NOT_FOUND) AuthorisableActionResult.NotFound() else staffMembersResponse.throwException()
    is ClientResult.Failure -> staffMembersResponse.throwException()
  }
}
