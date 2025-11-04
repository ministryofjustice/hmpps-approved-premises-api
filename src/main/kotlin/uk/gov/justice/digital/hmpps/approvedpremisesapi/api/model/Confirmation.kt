package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Confirmation(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("dateTime", required = true) val dateTime: java.time.Instant,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
