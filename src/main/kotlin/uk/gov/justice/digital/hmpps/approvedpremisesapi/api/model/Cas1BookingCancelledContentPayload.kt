package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param booking
 * @param cancellationReason
 * @param appealChangeRequestId
 */
data class Cas1BookingCancelledContentPayload(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

  @Schema(example = "null", description = "")
  @get:JsonProperty("appealChangeRequestId") val appealChangeRequestId: java.util.UUID? = null,
) : Cas1TimelineEventContentPayload
