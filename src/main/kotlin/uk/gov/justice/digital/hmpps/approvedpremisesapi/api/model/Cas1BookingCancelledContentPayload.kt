package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param booking
 * @param cancellationReason
 * @param appealChangeRequestId
 */
data class Cas1BookingCancelledContentPayload(

  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

  @get:JsonProperty("appealChangeRequestId") val appealChangeRequestId: java.util.UUID? = null,
) : Cas1TimelineEventContentPayload
