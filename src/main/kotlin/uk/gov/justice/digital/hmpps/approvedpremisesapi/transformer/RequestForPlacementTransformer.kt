package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1AuthorisedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer

@Component
class RequestForPlacementTransformer(
  private val jsonMapper: JsonMapper,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
) {
  fun transformPlacementApplicationEntityToApi(
    placementApplicationEntity: PlacementApplicationEntity,
    canBeDirectlyWithdrawn: Boolean,
  ): RequestForPlacement {
    val statusAndWhen = placementApplicationEntity.getStatus()
    val requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = placementApplicationEntity.expectedArrival!!,
      arrivalFlexible = placementApplicationEntity.expectedArrivalFlexible,
      duration = placementApplicationEntity.requestedDuration,
    )
    val authorisedPlacementPeriod = placementApplicationEntity.authorisedDuration?.let {
      Cas1AuthorisedPlacementPeriod(
        arrival = placementApplicationEntity.expectedArrival!!,
        arrivalFlexible = placementApplicationEntity.expectedArrivalFlexible,
        duration = it,
      )
    }
    val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = requestedPlacementPeriod.arrival,
      arrivalFlexible = requestedPlacementPeriod.arrivalFlexible,
      duration = authorisedPlacementPeriod?.duration ?: requestedPlacementPeriod.duration,
    )
    return RequestForPlacement(
      id = placementApplicationEntity.id,
      createdByUserId = placementApplicationEntity.createdByUser.id,
      createdAt = placementApplicationEntity.createdAt.toInstant(),
      isWithdrawn = placementApplicationEntity.isWithdrawn,
      type = if (placementApplicationEntity.automatic) {
        RequestForPlacementType.automatic
      } else {
        RequestForPlacementType.manual
      },
      canonicalPlacementPeriod = canonicalPlacementPeriod,
      requestedPlacementPeriod = requestedPlacementPeriod,
      authorisedPlacementPeriod = authorisedPlacementPeriod,
      submittedAt = placementApplicationEntity.submittedAt?.toInstant(),
      requestReviewedAt = placementApplicationEntity.decisionMadeAt?.toInstant(),
      document = placementApplicationEntity.document?.let(jsonMapper::readTree),
      canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      withdrawalReason = placementApplicationEntity.withdrawalReason?.apiValue,
      withdrawalDate = placementApplicationEntity.withdrawalOccurredAt,
      rejectionReason = placementApplicationEntity.decisionSummary.takeIf { placementApplicationEntity.decision == PlacementApplicationDecision.REJECTED },
      status = statusAndWhen.status.toApi(),
      statusSetDate = statusAndWhen.occurredAt,
      sentenceType = placementApplicationEntity.sentenceType?.let { SentenceTypeOption.valueOf(it) },
      releaseType = placementApplicationEntity.releaseType?.apiType,
      situation = placementApplicationEntity.situation?.let { SituationOption.valueOf(it) },
      decision = placementApplicationEntity.decision,
      submittedBy = cas1AssessmentTransformer.transformToStaffDto(placementApplicationEntity.createdByUser),
    )
  }

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
    val statusAndWhen = placementRequestEntity.getStatus()
    val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = placementRequestEntity.expectedArrival,
      arrivalFlexible = null,
      duration = placementRequestEntity.duration,
    )
    val requestedPlacementPeriod = Cas1RequestedPlacementPeriod(
      arrival = placementRequestEntity.expectedArrival,
      arrivalFlexible = null,
      duration = placementRequestEntity.duration,
    )
    val authorisedPlacementPeriod = Cas1AuthorisedPlacementPeriod(
      arrival = placementRequestEntity.expectedArrival,
      arrivalFlexible = null,
      duration = placementRequestEntity.duration,
    )
    return RequestForPlacement(
      id = placementRequestEntity.id,
      createdByUserId = placementRequestEntity.application.createdByUser.id,
      createdAt = placementRequestEntity.createdAt.toInstant(),
      isWithdrawn = placementRequestEntity.isWithdrawn,
      type = RequestForPlacementType.automatic,
      canonicalPlacementPeriod = canonicalPlacementPeriod,
      requestedPlacementPeriod = requestedPlacementPeriod,
      authorisedPlacementPeriod = authorisedPlacementPeriod,
      submittedAt = placementRequestEntity.createdAt.toInstant(),
      requestReviewedAt = placementRequestEntity.assessment.submittedAt?.toInstant(),
      document = null,
      canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      withdrawalReason = placementRequestEntity.withdrawalReason?.apiValue,
      withdrawalDate = placementRequestEntity.withdrawalOccurredAt,
      status = statusAndWhen.status.toApi(),
      statusSetDate = statusAndWhen.occurredAt,
      sentenceType = application.sentenceType?.let { SentenceTypeOption.valueOf(it) },
      releaseType = application.releaseType?.apiType,
      situation = application.situation?.let { SituationOption.valueOf(it) },
      decision = PlacementApplicationDecision.ACCEPTED,
      submittedBy = cas1AssessmentTransformer.transformToStaffDto(application.createdByUser),
    )
  }

  private fun PlacementApplicationEntity.Status.toApi() = when (this) {
    PlacementApplicationEntity.Status.REQUEST_UNSUBMITTED -> RequestForPlacementStatus.requestUnsubmitted
    PlacementApplicationEntity.Status.REQUEST_REJECTED -> RequestForPlacementStatus.requestRejected
    PlacementApplicationEntity.Status.REQUEST_SUBMITTED -> RequestForPlacementStatus.requestSubmitted
    PlacementApplicationEntity.Status.AWAITING_MATCH -> RequestForPlacementStatus.awaitingMatch
    PlacementApplicationEntity.Status.REQUEST_WITHDRAWN -> RequestForPlacementStatus.requestWithdrawn
    PlacementApplicationEntity.Status.PLACEMENT_BOOKED -> RequestForPlacementStatus.placementBooked
  }

  private fun PlacementRequestEntity.Status.toApi() = when (this) {
    PlacementRequestEntity.Status.AWAITING_MATCH -> RequestForPlacementStatus.awaitingMatch
    PlacementRequestEntity.Status.REQUEST_WITHDRAWN -> RequestForPlacementStatus.requestWithdrawn
    PlacementRequestEntity.Status.PLACEMENT_BOOKED -> RequestForPlacementStatus.placementBooked
  }
}
