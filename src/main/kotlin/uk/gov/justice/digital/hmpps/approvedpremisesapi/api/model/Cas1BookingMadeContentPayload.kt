package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param booking
 * @param eventNumber
 * @param transferredFrom
 */
data class Cas1BookingMadeContentPayload(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("eventNumber", required = true) val eventNumber: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

  @Schema(example = "null", description = "")
  @get:JsonProperty("transferredFrom") val transferredFrom: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
