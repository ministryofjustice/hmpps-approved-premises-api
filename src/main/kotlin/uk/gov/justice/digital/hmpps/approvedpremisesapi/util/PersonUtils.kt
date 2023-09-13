package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
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

fun OffenderService.getInfoForPersonOrThrow(crn: String, user: UserEntity): PersonInfoResult.Success {
  val personInfo = this.getInfoForPerson(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))
  if (personInfo is PersonInfoResult.NotFound) throw NotFoundProblem(crn, "Offender")

  return personInfo as PersonInfoResult.Success
}

fun OffenderService.getInfoForPersonOrThrowInternalServerError(crn: String, user: UserEntity): PersonInfoResult.Success {
  val personInfo = this.getInfoForPerson(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))
  if (personInfo is PersonInfoResult.NotFound) throw InternalServerErrorProblem("Unable to get Person via crn: $crn")

  return personInfo as PersonInfoResult.Success
}

fun OffenderService.getNaiveInfoForPersonOrThrowInternalServerError(crn: String): PersonInfoResult.Success {
  val personInfo = this.getNaiveInfoForPerson(crn)
  if (personInfo is PersonInfoResult.NotFound) throw InternalServerErrorProblem("Unable to get Person via crn: $crn")

  return personInfo as PersonInfoResult.Success
}

fun OffenderService.getFullInfoForPersonOrThrow(crn: String, user: UserEntity): PersonInfoResult.Success.Full {
  val personInfo = this.getInfoForPerson(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))
  if (personInfo is PersonInfoResult.NotFound) throw NotFoundProblem(crn, "Offender")
  if (personInfo is PersonInfoResult.Success.Restricted) throw ForbiddenProblem()

  return personInfo as PersonInfoResult.Success.Full
}
