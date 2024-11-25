package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id If `type` is `\"manual\"`, provides the `PlacementApplication` ID. If `type` is `\"automatic\"` this field provides a `PlacementRequest` ID.
 * @param createdByUserId
 * @param createdAt
 * @param canBeDirectlyWithdrawn If true, the user making this request can withdraw this request for placement.  If false, it may still be possible to indirectly withdraw this request for placement by withdrawing the application.
 * @param isWithdrawn
 * @param type
 * @param placementDates
 * @param status
 * @param submittedAt
 * @param requestReviewedAt If `type` is `\"manual\"`, provides the value of `PlacementApplication.decisionMadeAt`. If `type` is `\"automatic\"` this field provides the value of `PlacementRequest.assessmentCompletedAt`.
 * @param document Any object that conforms to the current JSON schema for an application
 * @param withdrawalReason
 */
data class RequestForPlacement(

  @Schema(example = "null", required = true, description = "If `type` is `\"manual\"`, provides the `PlacementApplication` ID. If `type` is `\"automatic\"` this field provides a `PlacementRequest` ID. ")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "If true, the user making this request can withdraw this request for placement.  If false, it may still be possible to indirectly withdraw this request for placement by withdrawing the application. ")
  @get:JsonProperty("canBeDirectlyWithdrawn", required = true) val canBeDirectlyWithdrawn: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: RequestForPlacementType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<PlacementDates>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: RequestForPlacementStatus,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "If `type` is `\"manual\"`, provides the value of `PlacementApplication.decisionMadeAt`. If `type` is `\"automatic\"` this field provides the value of `PlacementRequest.assessmentCompletedAt`. ")
  @get:JsonProperty("requestReviewedAt") val requestReviewedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("withdrawalReason") val withdrawalReason: WithdrawPlacementRequestReason? = null,
)
