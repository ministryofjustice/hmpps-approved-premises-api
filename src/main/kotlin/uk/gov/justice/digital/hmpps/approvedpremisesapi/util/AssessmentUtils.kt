package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem

fun List<AssessmentSummary>.sort(sortDirection: SortDirection, sortField: AssessmentSortField): List<AssessmentSummary> {
  val comparator = Comparator<AssessmentSummary> { a, b ->
    val ascendingCompare = when (sortField) {
      AssessmentSortField.personName -> compareValues((a.person as? FullPerson)?.name, (b.person as? FullPerson)?.name)
      AssessmentSortField.personCrn -> compareValues(a.person.crn, b.person.crn)
      AssessmentSortField.assessmentArrivalDate -> compareValues(a.arrivalDate, b.arrivalDate)
      AssessmentSortField.assessmentStatus -> when {
        a is TemporaryAccommodationAssessmentSummary && b is TemporaryAccommodationAssessmentSummary -> compareValues(a.status, b.status)
        else -> throw RuntimeException("Cannot compare values of types ${a::class.qualifiedName} and ${b::class.qualifiedName} due to incomparable status types.")
      }
      AssessmentSortField.assessmentCreatedAt -> compareValues(a.createdAt, b.createdAt)
      AssessmentSortField.assessmentDueAt -> throw BadRequestProblem(errorDetail = "Sorting by due date is not supported for CAS3")
    }

    when (sortDirection) {
      SortDirection.asc, null -> ascendingCompare
      SortDirection.desc -> -ascendingCompare
    }
  }

  return this.sortedWith(comparator)
}
