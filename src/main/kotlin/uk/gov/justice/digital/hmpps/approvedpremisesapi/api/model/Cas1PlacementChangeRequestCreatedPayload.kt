package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param changeRequestId
 * @param booking
 * @param reason
 * @param changeRequestType
 */
data class Cas1PlacementChangeRequestCreatedPayload(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("changeRequestId", required = true) val changeRequestId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("changeRequestType", required = true) val changeRequestType: Cas1ChangeRequestType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,
) : Cas1TimelineEventContentPayload
