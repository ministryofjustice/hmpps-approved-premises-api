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
  ) = RequestForPlacement(
    id = placementApplicationEntity.id,
    createdByUserId = placementApplicationEntity.createdByUser.id,
    createdAt = placementApplicationEntity.createdAt.toInstant(),
    isWithdrawn = placementApplicationEntity.isWithdrawn(),
    type = RequestForPlacementType.manual,
    placementDates = placementApplicationEntity.placementDates.map { it.toPlacementDates() },
    submittedAt = placementApplicationEntity.submittedAt?.toInstant(),
    requestReviewedAt = placementApplicationEntity.decisionMadeAt?.toInstant(),
    document = placementApplicationEntity.document?.let(objectMapper::readTree),
    withdrawalReason = placementApplicationEntity.withdrawalReason?.apiValue,
    status = placementApplicationEntity.deriveStatus(),
  )

  fun transformPlacementRequestEntityToApi(
    placementRequestEntity: PlacementRequestEntity,
  ) = RequestForPlacement(
    id = placementRequestEntity.id,
    createdByUserId = placementRequestEntity.application.createdByUser.id,
    createdAt = placementRequestEntity.createdAt.toInstant(),
    isWithdrawn = placementRequestEntity.isWithdrawn,
    type = RequestForPlacementType.automatic,
    placementDates = listOf(
      PlacementDates(
        expectedArrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
      ),
    ),
    submittedAt = placementRequestEntity.createdAt.toInstant(),
    requestReviewedAt = placementRequestEntity.assessment.submittedAt?.toInstant(),
    document = null,
    withdrawalReason = placementRequestEntity.withdrawalReason?.apiValue,
    status = placementRequestEntity.deriveStatus(),
  )

  private fun PlacementDateEntity.toPlacementDates() = PlacementDates(
    expectedArrival = expectedArrival,
    duration = duration,
  )

  private fun PlacementApplicationEntity.deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn() -> RequestForPlacementStatus.requestWithdrawn
    this.application.getLatestBooking() != null -> RequestForPlacementStatus.placementBooked

    this.decision == PlacementApplicationDecision.REJECTED -> RequestForPlacementStatus.requestRejected

    this.isSubmitted() -> RequestForPlacementStatus.requestSubmitted

    else -> RequestForPlacementStatus.awaitingMatch
  }

  private fun PlacementRequestEntity.deriveStatus(): RequestForPlacementStatus {
    val placementApplication = this.placementApplication

    return when {
      this.isWithdrawn -> RequestForPlacementStatus.requestWithdrawn
      this.hasActiveBooking() -> RequestForPlacementStatus.placementBooked

      placementApplication != null && placementApplication.decision == PlacementApplicationDecision.REJECTED ->
        RequestForPlacementStatus.requestRejected

      placementApplication != null && placementApplication.isSubmitted() ->
        RequestForPlacementStatus.requestSubmitted

      else -> RequestForPlacementStatus.awaitingMatch
    }
  }
}
