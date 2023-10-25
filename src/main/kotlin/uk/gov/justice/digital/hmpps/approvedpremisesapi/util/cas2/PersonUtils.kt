package uk.gov.justice.digital.hmpps.approvedpremisesapi.util.cas2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService

fun OffenderService.getInfoForPersonOrThrowInternalServerError(crn: String):
  PersonInfoResult.Success {
  val personInfo = this.getInfoForPerson(crn)
  if (personInfo is PersonInfoResult.NotFound) throw InternalServerErrorProblem("Unable to get Person via crn: $crn")

  return personInfo as PersonInfoResult.Success
}

fun OffenderService.getFullInfoForPersonOrThrow(crn: String):
  PersonInfoResult.Success.Full {
  val personInfo = this.getInfoForPerson(crn)
  if (personInfo is PersonInfoResult.NotFound) throw NotFoundProblem(crn, "Offender")
  if (personInfo is PersonInfoResult.Success.Restricted) throw ForbiddenProblem()

  return personInfo as PersonInfoResult.Success.Full
}
