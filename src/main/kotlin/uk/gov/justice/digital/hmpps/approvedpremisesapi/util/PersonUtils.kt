package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.into
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun getPersonDetailsForCrn(
  log: Logger,
  crn: String,
  deliusUsername: String,
  offenderService: OffenderService,
  ignoreLao: Boolean = false,
): Pair<OffenderDetailSummary, InmateDetail?>? {
  return when (val personDetailsResult = tryGetPersonDetailsForCrn(log, crn, deliusUsername, offenderService, ignoreLao)) {
    is AuthorisableActionResult.Success -> personDetailsResult.entity
    is AuthorisableActionResult.NotFound -> null
    is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
  }
}

fun tryGetPersonDetailsForCrn(
  log: Logger,
  crn: String,
  deliusUsername: String,
  offenderService: OffenderService,
  ignoreLao: Boolean,
): AuthorisableActionResult<Pair<OffenderDetailSummary, InmateDetail?>> {
  val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, deliusUsername, ignoreLao)) {
    is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    is AuthorisableActionResult.NotFound -> {
      log.error("Could not get Offender Details for CRN: $crn")
      return offenderDetailsResult.into()
    }
    is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
  }

  val inmateDetails = getInmateDetail(offenderDetails, offenderService)

  return AuthorisableActionResult.Success(Pair(offenderDetails, inmateDetails))
}

fun getInmateDetail(offenderDetails: OffenderDetailSummary, offenderService: OffenderService): InmateDetail? {
  val nomsNumber = offenderDetails.otherIds.nomsNumber

  if (nomsNumber.isNullOrEmpty()) {
    return null
  }

  return when (
    val inmateDetailsResult =
      offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber)
  ) {
    is AuthorisableActionResult.Success -> inmateDetailsResult.entity
    is AuthorisableActionResult.NotFound -> null
    is AuthorisableActionResult.Unauthorised -> null
  }
}
