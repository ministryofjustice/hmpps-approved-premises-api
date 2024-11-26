package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class StaffMemberService(private val apDeliusContextApiClient: ApDeliusContextApiClient) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getStaffMemberByCodeForPremise(code: String, qCode: String): CasResult<StaffMember> {
    val premisesStaffMembers =
      when (val premisesStaffMembersResult = getStaffMembersForQCode(qCode)) {
        is CasResult.Error -> return premisesStaffMembersResult.reviseType()
        is CasResult.Success -> premisesStaffMembersResult.value
      }

    val staffMember = premisesStaffMembers.content.firstOrNull { it.code == code }

    return if (staffMember != null) {
      CasResult.Success(staffMember)
    } else {
      log.warn("Whilst a result was returning for qCode $qCode, no staff member with code $code was found in it")
      CasResult.NotFound("Staff Code", code)
    }
  }

  fun getStaffMembersForQCode(qCode: String) = when (val staffMembersResponse = apDeliusContextApiClient.getStaffMembers(qCode)) {
    is ClientResult.Success -> CasResult.Success(staffMembersResponse.body)
    is ClientResult.Failure.StatusCode -> when (staffMembersResponse.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Team", qCode)
      HttpStatus.UNAUTHORIZED -> CasResult.Unauthorised()
      else -> staffMembersResponse.throwException()
    }
    is ClientResult.Failure -> staffMembersResponse.throwException()
  }
}
