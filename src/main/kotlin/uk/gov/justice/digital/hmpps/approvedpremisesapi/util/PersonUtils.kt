package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun getPersonDetailsForCrn(
  log: Logger,
  crn: String,
  deliusUsername: String,
  offenderService: OffenderService,
): Pair<OffenderDetailSummary, InmateDetail>? {
  val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, deliusUsername)) {
    is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    is AuthorisableActionResult.NotFound -> {
      log.error("Could not get Offender Details for CRN: $crn")
      return null
    }

    is AuthorisableActionResult.Unauthorised -> return null
  }

  if (offenderDetails.otherIds.nomsNumber == null) {
    log.error("No NOMS number for CRN: $crn")
    return null
  }

  val inmateDetails = when (
    val inmateDetailsResult =
      offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber)
  ) {
    is AuthorisableActionResult.Success -> inmateDetailsResult.entity
    is AuthorisableActionResult.NotFound -> {
      log.error("Could not get Inmate Details for NOMS number: ${offenderDetails.otherIds.nomsNumber}")
      return null
    }

    is AuthorisableActionResult.Unauthorised -> return null
  }

  return Pair(offenderDetails, inmateDetails)
}
