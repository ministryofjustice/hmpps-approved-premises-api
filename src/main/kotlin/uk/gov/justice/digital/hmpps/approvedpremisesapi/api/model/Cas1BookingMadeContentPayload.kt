package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1BookingMadeContentPayload(

  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @get:JsonProperty("eventNumber", required = true) val eventNumber: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,

  @get:JsonProperty("transferredFrom") val transferredFrom: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
