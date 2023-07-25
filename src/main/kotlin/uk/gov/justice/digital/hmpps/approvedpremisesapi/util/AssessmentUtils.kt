package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

fun mapAndTransformAssessmentSummaries(
  log: Logger,
  assessments: List<DomainAssessmentSummary>,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (DomainAssessmentSummary, OffenderDetailSummary, InmateDetail?) -> AssessmentSummary,
  ignoreLao: Boolean = false,
  sortOrder: SortOrder? = null,
  sortField: AssessmentSortField? = null,
): List<AssessmentSummary> {
  return assessments.map {
    transformAssessmentSummary(log, it, deliusUsername, offenderService, transformer, ignoreLao)
  }.sort(sortOrder, sortField)
}

fun <T> transformAssessmentSummary(
  log: Logger,
  assessment: DomainAssessmentSummary,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (DomainAssessmentSummary, OffenderDetailSummary, InmateDetail?) -> T,
  ignoreLao: Boolean,
): T {
  val (offenderDetailSummary, inmateDetail) = getPersonDetailsForCrn(log, assessment.crn, deliusUsername, offenderService, ignoreLao)
    ?: throw NotFoundProblem(assessment.crn, "Offender")

  return transformer(assessment, offenderDetailSummary, inmateDetail)
}

private fun List<AssessmentSummary>.sort(sortOrder: SortOrder?, sortField: AssessmentSortField?): List<AssessmentSummary> {
  if (sortField != null) {
    val comparator = Comparator<AssessmentSummary> { a, b ->
      val ascendingCompare = when (sortField) {
        AssessmentSortField.personName -> compareValues(a.person.name, b.person.name)
        AssessmentSortField.personCrn -> compareValues(a.person.crn, b.person.crn)
        AssessmentSortField.assessmentArrivalDate -> compareValues(a.arrivalDate, b.arrivalDate)
        AssessmentSortField.assessmentStatus -> compareValues(a.status, b.status)
        AssessmentSortField.assessmentCreatedAt -> compareValues(a.createdAt, b.createdAt)
      }

      when (sortOrder) {
        SortOrder.ascending, null -> ascendingCompare
        SortOrder.descending -> -ascendingCompare
      }
    }

    return this.sortedWith(comparator)
  }

  return this
}
