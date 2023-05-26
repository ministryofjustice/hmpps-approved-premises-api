package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun <T> mapAndTransformAssessments(
  log: Logger,
  assessments: List<AssessmentEntity>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (AssessmentEntity, OffenderDetailSummary, InmateDetail) -> T,
): List<T> {
  return assessments.mapNotNull {
    val assessment = transformAssessment(log, it, deliusUsername, offenderService, transformer) ?: return@mapNotNull null

    assessment
  }
}

fun <T> transformAssessment(
  log: Logger,
  assessment: AssessmentEntity,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (AssessmentEntity, OffenderDetailSummary, InmateDetail) -> T,
): T {
  val (offenderDetailSummary, inmateDetail) = getPersonDetailsForCrn(log, assessment.application.crn, deliusUsername, offenderService)
    ?: throw NotFoundProblem(assessment.application.crn, "Offender")

  return transformer(assessment, offenderDetailSummary, inmateDetail)
}

fun <T> mapAndTransformAssessmentSummaries(
  log: Logger,
  assessments: List<DomainAssessmentSummary>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (DomainAssessmentSummary, OffenderDetailSummary, InmateDetail) -> T,
): List<T> {
  return assessments.mapNotNull {
    transformAssessmentSummary(log, it, deliusUsername, offenderService, transformer) ?: return@mapNotNull null
  }
}

fun <T> transformAssessmentSummary(
  log: Logger,
  assessment: DomainAssessmentSummary,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (DomainAssessmentSummary, OffenderDetailSummary, InmateDetail) -> T,
): T {
  val (offenderDetailSummary, inmateDetail) = getPersonDetailsForCrn(log, assessment.crn, deliusUsername, offenderService)
    ?: throw NotFoundProblem(assessment.crn, "Offender")

  return transformer(assessment, offenderDetailSummary, inmateDetail)
}
