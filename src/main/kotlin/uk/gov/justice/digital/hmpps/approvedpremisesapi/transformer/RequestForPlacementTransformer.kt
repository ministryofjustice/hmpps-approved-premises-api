package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
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
    type = RequestForPlacementType.manual,
    dates = placementApplicationEntity.placementDates()!!.toApiType(),
    placementDates = listOf(placementApplicationEntity.placementDates()!!.toApiType()),
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
      type = RequestForPlacementType.automatic,
      dates = PlacementDates(
        expectedArrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
      ),
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

  private fun PlacementApplicationEntity.deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn -> RequestForPlacementStatus.requestWithdrawn
    this.placementRequests.any { pr -> pr.hasActiveBooking() } -> RequestForPlacementStatus.placementBooked
    this.decision == PlacementApplicationDecision.REJECTED -> RequestForPlacementStatus.requestRejected
    this.decision == PlacementApplicationDecision.ACCEPTED -> RequestForPlacementStatus.awaitingMatch
    this.isSubmitted() -> RequestForPlacementStatus.requestSubmitted
    else -> RequestForPlacementStatus.requestUnsubmitted
  }

  private fun PlacementRequestEntity.deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn -> RequestForPlacementStatus.requestWithdrawn
    this.hasActiveBooking() -> RequestForPlacementStatus.placementBooked
    else -> RequestForPlacementStatus.awaitingMatch
  }
}
