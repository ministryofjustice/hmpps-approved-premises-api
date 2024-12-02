package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

@Component
class RequestForPlacementTransformer(
  private val objectMapper: ObjectMapper,
) {
  fun transformPlacementApplicationEntityToApi(
    placementApplicationEntity: PlacementApplicationEntity,
    canBeDirectlyWithdrawn: Boolean,
  ) = RequestForPlacement(
    id = placementApplicationEntity.id,
    createdByUserId = placementApplicationEntity.createdByUser.id,
    createdAt = placementApplicationEntity.createdAt.toInstant(),
    isWithdrawn = placementApplicationEntity.isWithdrawn,
    type = RequestForPlacementType.MANUAL,
    placementDates = placementApplicationEntity.placementDates.map { it.toPlacementDates() },
    submittedAt = placementApplicationEntity.submittedAt?.toInstant(),
    requestReviewedAt = placementApplicationEntity.decisionMadeAt?.toInstant(),
    document = placementApplicationEntity.document?.let(objectMapper::readTree),
    canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
    withdrawalReason = placementApplicationEntity.withdrawalReason?.apiValue,
    status = placementApplicationEntity.deriveStatus(),
  )

  /**
   * This should only be used for placement requests for the application's arrival date.
   *
   * This will only exist once the application has been approved, as such, the potential
   * status' for this entity are limited.
   *
   * For more information, For more information, see [PlacementRequestEntity.isForApplicationsArrivalDate]
   */
  fun transformPlacementRequestEntityToApi(
    placementRequestEntity: PlacementRequestEntity,
    canBeDirectlyWithdrawn: Boolean,
  ): RequestForPlacement {
    check(placementRequestEntity.isForApplicationsArrivalDate()) {
      "Can only transform placement requests that are for the application's arrival date"
    }

    return RequestForPlacement(
      id = placementRequestEntity.id,
      createdByUserId = placementRequestEntity.application.createdByUser.id,
      createdAt = placementRequestEntity.createdAt.toInstant(),
      isWithdrawn = placementRequestEntity.isWithdrawn,
      type = RequestForPlacementType.AUTOMATIC,
      placementDates = listOf(
        PlacementDates(
          expectedArrival = placementRequestEntity.expectedArrival,
          duration = placementRequestEntity.duration,
        ),
      ),
      submittedAt = placementRequestEntity.createdAt.toInstant(),
      requestReviewedAt = placementRequestEntity.assessment.submittedAt?.toInstant(),
      document = null,
      canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      withdrawalReason = placementRequestEntity.withdrawalReason?.apiValue,
      status = placementRequestEntity.deriveStatus(),
    )
  }

  private fun PlacementDateEntity.toPlacementDates() = PlacementDates(
    expectedArrival = expectedArrival,
    duration = duration,
  )

  private fun PlacementApplicationEntity.deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn -> RequestForPlacementStatus.REQUEST_WITHDRAWN
    this.placementRequests.any { pr -> pr.hasActiveBooking() } -> RequestForPlacementStatus.PLACEMENT_BOOKED
    this.decision == PlacementApplicationDecision.REJECTED -> RequestForPlacementStatus.REQUEST_REJECTED
    this.decision == PlacementApplicationDecision.ACCEPTED -> RequestForPlacementStatus.AWAITING_MATCH
    this.isSubmitted() -> RequestForPlacementStatus.REQUEST_SUBMITTED
    else -> RequestForPlacementStatus.REQUEST_UNSUBMITTED
  }

  private fun PlacementRequestEntity.deriveStatus(): RequestForPlacementStatus {
    return when {
      this.isWithdrawn -> RequestForPlacementStatus.REQUEST_WITHDRAWN
      this.hasActiveBooking() -> RequestForPlacementStatus.PLACEMENT_BOOKED
      else -> RequestForPlacementStatus.AWAITING_MATCH
    }
  }
}
