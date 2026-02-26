package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
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
    type = if (placementApplicationEntity.automatic) {
      RequestForPlacementType.automatic
    } else {
      RequestForPlacementType.manual
    },
    placementDates = listOf(placementApplicationEntity.placementDates()!!.toApiType()),
    requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = placementApplicationEntity.expectedArrival!!,
      arrivalFlexible = placementApplicationEntity.expectedArrivalFlexible,
      duration = placementApplicationEntity.requestedDuration!!,
    ),
    authorisedPlacementPeriod = placementApplicationEntity.authorisedDuration?.let {
      Cas1RequestedPlacementPeriod(
        arrival = placementApplicationEntity.expectedArrival!!,
        arrivalFlexible = placementApplicationEntity.expectedArrivalFlexible,
        duration = it,
      )
    },
    submittedAt = placementApplicationEntity.submittedAt?.toInstant(),
    requestReviewedAt = placementApplicationEntity.decisionMadeAt?.toInstant(),
    document = placementApplicationEntity.document?.let(objectMapper::readTree),
    canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
    withdrawalReason = placementApplicationEntity.withdrawalReason?.apiValue,
    status = placementApplicationEntity.deriveStatus(),
    sentenceType = placementApplicationEntity.sentenceType?.let { SentenceTypeOption.valueOf(it) },
    releaseType = placementApplicationEntity.releaseType?.let { ReleaseTypeOption.valueOf(it) },
    situation = placementApplicationEntity.situation?.let { SituationOption.valueOf(it) },
  )

  /**
   * This should only be used for placement requests for the application's arrival date.
   *
   * This will only exist once the application has been approved, as such, the potential
   * status' for this entity are limited.
   *
   * For more information, For more information, see [PlacementRequestEntity.isForLegacyInitialRequestForPlacement]
   */
  fun transformPlacementRequestEntityToApi(
    placementRequestEntity: PlacementRequestEntity,
    canBeDirectlyWithdrawn: Boolean,
  ): RequestForPlacement {
    check(placementRequestEntity.isForLegacyInitialRequestForPlacement()) {
      "Can only transform placement requests that are for the application's arrival date"
    }

    val application = placementRequestEntity.application

    return RequestForPlacement(
      id = placementRequestEntity.id,
      createdByUserId = placementRequestEntity.application.createdByUser.id,
      createdAt = placementRequestEntity.createdAt.toInstant(),
      isWithdrawn = placementRequestEntity.isWithdrawn,
      type = RequestForPlacementType.automatic,
      requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = placementRequestEntity.expectedArrival,
        arrivalFlexible = null,
        duration = placementRequestEntity.duration,
      ),
      authorisedPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = placementRequestEntity.expectedArrival,
        arrivalFlexible = null,
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
      sentenceType = application.sentenceType?.let { SentenceTypeOption.valueOf(it) },
      releaseType = application.releaseType?.let { ReleaseTypeOption.valueOf(it) },
      situation = application.situation?.let { SituationOption.valueOf(it) },
    )
  }


  private fun PlacementRequestEntity.deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn -> RequestForPlacementStatus.requestWithdrawn
    this.hasActiveBooking() -> RequestForPlacementStatus.placementBooked
    else -> RequestForPlacementStatus.awaitingMatch
  }
}
