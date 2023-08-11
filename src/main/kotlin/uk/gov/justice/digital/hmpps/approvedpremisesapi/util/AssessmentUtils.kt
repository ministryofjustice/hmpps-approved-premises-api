package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
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
  statuses: List<AssessmentStatus>? = null,
): List<AssessmentSummary> {
  return assessments.mapNotNull {
    transformAssessmentSummary(log, it, deliusUsername, offenderService, transformer, ignoreLao)
  }
    .sort(sortOrder, sortField)
    .filterByStatuses(statuses)
}

fun <T> transformAssessmentSummary(
  log: Logger,
  assessment: DomainAssessmentSummary,
  deliusUsername: String,
  offenderService: OffenderService,
  transformer: (DomainAssessmentSummary, OffenderDetailSummary, InmateDetail?) -> T,
  ignoreLao: Boolean,
): T? {
  val (offenderDetailSummary, inmateDetail) = when (
    val personDetailsResult = tryGetPersonDetailsForCrn(log, assessment.crn, deliusUsername, offenderService, ignoreLao)
  ) {
    is AuthorisableActionResult.Success -> personDetailsResult.entity
    is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessment.crn, "Offender")
    is AuthorisableActionResult.Unauthorised -> return null
  }

  return transformer(assessment, offenderDetailSummary, inmateDetail)
}

private fun List<AssessmentSummary>.sort(sortOrder: SortOrder?, sortField: AssessmentSortField?): List<AssessmentSummary> {
  if (sortField != null) {
    val comparator = Comparator<AssessmentSummary> { a, b ->
      val ascendingCompare = when (sortField) {
        AssessmentSortField.personName -> compareValues(a.person.name, b.person.name)
        AssessmentSortField.personCrn -> compareValues(a.person.crn, b.person.crn)
        AssessmentSortField.assessmentArrivalDate -> compareValues(a.arrivalDate, b.arrivalDate)
        AssessmentSortField.assessmentStatus -> when {
          a is ApprovedPremisesAssessmentSummary && b is ApprovedPremisesAssessmentSummary -> compareValues(a.status, b.status)
          a is TemporaryAccommodationAssessmentSummary && b is TemporaryAccommodationAssessmentSummary -> compareValues(a.status, b.status)
          else -> throw RuntimeException("Cannot compare values of types ${a::class.qualifiedName} and ${b::class.qualifiedName} due to incomparable status types.")
        }
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

private fun List<AssessmentSummary>.filterByStatuses(statuses: List<AssessmentStatus>?): List<AssessmentSummary> {
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
