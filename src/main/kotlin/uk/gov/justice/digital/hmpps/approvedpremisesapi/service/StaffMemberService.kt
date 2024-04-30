package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

@Service
class StaffMemberService(private val apDeliusContextApiClient: ApDeliusContextApiClient) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getStaffMemberByCode(code: String, qCode: String): AuthorisableActionResult<StaffMember> {
    val premisesStaffMembers = when (val premisesStaffMembersResult = getStaffMembersForQCode(qCode)) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound("QCode", qCode)
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> premisesStaffMembersResult.entity
    }

    val staffMember = premisesStaffMembers.content.firstOrNull { it.code == code }

    return if (staffMember != null) {
      AuthorisableActionResult.Success(staffMember)
    } else {
      log.warn("Whilst a result was returning for qCode $qCode, no staff member with code $code was found in it")
      AuthorisableActionResult.NotFound("Staff Code", code)
    }
  }

  fun getStaffMembersForQCode(qCode: String) = when (val staffMembersResponse = apDeliusContextApiClient.getStaffMembers(qCode)) {
    is ClientResult.Success -> AuthorisableActionResult.Success(staffMembersResponse.body)
    is ClientResult.Failure.StatusCode -> when (staffMembersResponse.status) {
      HttpStatus.NOT_FOUND -> AuthorisableActionResult.NotFound()
      HttpStatus.UNAUTHORIZED -> AuthorisableActionResult.Unauthorised()
      else -> staffMembersResponse.throwException()
    }
    is ClientResult.Failure -> staffMembersResponse.throwException()
  }
}
