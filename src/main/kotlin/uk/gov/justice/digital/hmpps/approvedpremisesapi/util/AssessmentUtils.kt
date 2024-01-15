package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary

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
    }

    when (sortDirection) {
      SortDirection.asc, null -> ascendingCompare
      SortDirection.desc -> -ascendingCompare
    }
  }

  return this.sortedWith(comparator)
}

fun List<AssessmentSummary>.filterByStatuses(statuses: List<AssessmentStatus>?): List<AssessmentSummary> {
  if (statuses == null) {
    return this
  }

  val approvedPremisesStatuses = statuses.mapNotNull { it.toApprovedPremisesAssessmentStatus() }.toSet()
  val temporaryAccommodationStatuses = statuses.mapNotNull { it.toTemporaryAccommodationAssessmentStatus() }.toSet()

  return this.filter {
    when (it) {
      is ApprovedPremisesAssessmentSummary -> approvedPremisesStatuses.contains(it.status)
      is TemporaryAccommodationAssessmentSummary -> temporaryAccommodationStatuses.contains(it.status)
      else -> throw RuntimeException("Unknown assessment summary type '${it::class.qualifiedName}'; could not narrow AssessmentStatus enum to its corresponding service-specific enum.")
    }
  }
}

private fun AssessmentStatus.toApprovedPremisesAssessmentStatus() = when (this) {
  AssessmentStatus.cas1AwaitingResponse -> ApprovedPremisesAssessmentStatus.awaitingResponse
  AssessmentStatus.cas1Completed -> ApprovedPremisesAssessmentStatus.completed
  AssessmentStatus.cas1Reallocated -> ApprovedPremisesAssessmentStatus.reallocated
  AssessmentStatus.cas1InProgress -> ApprovedPremisesAssessmentStatus.inProgress
  AssessmentStatus.cas1NotStarted -> ApprovedPremisesAssessmentStatus.notStarted
  else -> null
}

private fun AssessmentStatus.toTemporaryAccommodationAssessmentStatus() = when (this) {
  AssessmentStatus.cas3Unallocated -> TemporaryAccommodationAssessmentStatus.unallocated
  AssessmentStatus.cas3InReview -> TemporaryAccommodationAssessmentStatus.inReview
  AssessmentStatus.cas3ReadyToPlace -> TemporaryAccommodationAssessmentStatus.readyToPlace
  AssessmentStatus.cas3Closed -> TemporaryAccommodationAssessmentStatus.closed
  AssessmentStatus.cas3Rejected -> TemporaryAccommodationAssessmentStatus.rejected
  else -> null
}
