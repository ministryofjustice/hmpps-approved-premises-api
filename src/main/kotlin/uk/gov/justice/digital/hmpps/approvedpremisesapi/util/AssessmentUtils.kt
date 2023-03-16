package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun mapAndTransformAssessments(
  log: Logger,
  assessments: List<AssessmentEntity>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (AssessmentEntity, OffenderDetailSummary, InmateDetail) -> Any
): List<Any> {
  return assessments.mapNotNull {
    val personDetail = getPersonDetailsForCrn(log, it.application.crn, deliusUsername, offenderService)

    if (personDetail === null) {
      return@mapNotNull null
    }

    transformer(it, personDetail.first, personDetail.second)
  }
}
