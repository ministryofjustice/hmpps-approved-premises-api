package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1SpaceBookingCancellation(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @get:JsonProperty("recordedAt", required = true) val recordedAt: java.time.Instant,

  @get:JsonProperty("reason", required = true) val reason: CancellationReason,

  @get:JsonProperty("reason_notes") val reasonNotes: kotlin.String? = null,
)
