package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun mapAndTransformAssessments(
  log: Logger,
  assessments: List<AssessmentEntity>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (AssessmentEntity, OffenderDetailSummary, InmateDetail) -> Any
): List<Any> {
  return assessments.mapNotNull {
    val applicationCrn = it.application.crn

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(applicationCrn, deliusUsername)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.NotFound -> {
        log.error("Could not get Offender Details for CRN: $applicationCrn")
        return@mapNotNull null
      }

      is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      log.error("No NOMS number for CRN: $applicationCrn")
      return@mapNotNull null
    }

    val inmateDetails = when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)) {
      is AuthorisableActionResult.Success -> inmateDetailsResult.entity
      is AuthorisableActionResult.NotFound -> {
        log.error("Could not get Inmate Details for NOMS number: ${offenderDetails.otherIds.nomsNumber}")
        return@mapNotNull null
      }

      is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
    }

    transformer(it, offenderDetails, inmateDetails)
  }
}
