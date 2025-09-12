package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class StaffMemberService(
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
) {

  fun getStaffMemberByCode(code: String) = when (val staffMembersResponse = apDeliusContextApiClient.getStaffDetailByStaffCode(code)) {
    is ClientResult.Success -> CasResult.Success(staffMembersResponse.body)
    is ClientResult.Failure.StatusCode -> when (staffMembersResponse.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("StaffMember", code)
      HttpStatus.UNAUTHORIZED -> CasResult.Unauthorised()
      else -> staffMembersResponse.throwException()
    }
    is ClientResult.Failure -> staffMembersResponse.throwException()
  }
}
