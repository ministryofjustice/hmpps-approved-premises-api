package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

@Service
class PersonService(
  private val offenderService: OffenderService
) {

  fun getPersonByCrn(crn: String, userDistinguishedName: String): AuthorisableActionResult<Pair<OffenderDetailSummary, InmateDetail>> {
    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, userDistinguishedName)) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised(crn, "Person")
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw RuntimeException("No nomsNumber present for CRN: `$crn`")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val inmateDetail = when (val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(nomsNumber)) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound(nomsNumber, "Inmate")
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised(crn, "Inmate")
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    return AuthorisableActionResult.Success(
      Pair(offenderDetails, inmateDetail)
    )
  }
}
