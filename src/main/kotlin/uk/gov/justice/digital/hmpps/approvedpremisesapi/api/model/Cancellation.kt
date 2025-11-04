package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cancellation(

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @get:JsonProperty("reason", required = true) val reason: CancellationReason,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("premisesName", required = true) val premisesName: kotlin.String,

  @get:JsonProperty("id") val id: java.util.UUID? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null,
)
