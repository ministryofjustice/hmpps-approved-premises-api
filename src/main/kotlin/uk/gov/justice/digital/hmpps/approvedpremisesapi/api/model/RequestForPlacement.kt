package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import java.time.LocalDate

data class RequestForPlacement(
  @Schema(description = "If `type` is `\"manual\"`, provides the `PlacementApplication` ID. If `type` is `\"automatic\"` this field provides a `PlacementRequest` ID. ")
  val id: java.util.UUID,
  val createdByUserId: java.util.UUID,
  val createdAt: java.time.Instant,
  @Schema(description = "If true, the user making this request can withdraw this request for placement.  If false, it may still be possible to indirectly withdraw this request for placement by withdrawing the application. ")
  val canBeDirectlyWithdrawn: Boolean,
  val isWithdrawn: Boolean,
  val type: RequestForPlacementType,
  @Schema(description = "Deprecated. Requests for placements only have one set of placement dates, use 'requestedPlacementPeriod' or 'authorisedPlacementPeriod' instead", deprecated = true)
  val placementDates: List<PlacementDates>,
  val requestedPlacementPeriod: Cas1RequestedPlacementPeriod,
  val authorisedPlacementPeriod: Cas1RequestedPlacementPeriod?,
  val status: RequestForPlacementStatus,
  val statusSetDate: LocalDate,
  val submittedAt: java.time.Instant? = null,
  @Schema(description = "If `type` is `\"manual\"`, provides the value of `PlacementApplication.decisionMadeAt`. If `type` is `\"automatic\"` this field provides the value of `PlacementRequest.assessmentCompletedAt`. ")
  val requestReviewedAt: java.time.Instant? = null,
  val document: Any? = null,
  val withdrawalReason: WithdrawPlacementRequestReason? = null,
  val sentenceType: SentenceTypeOption? = null,
  val releaseType: ReleaseTypeOption? = null,
  val situation: SituationOption? = null,
  var placements: List<Cas1SpaceBookingShortSummary> = emptyList(),
)
