package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Departure(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("dateTime", required = true) val dateTime: java.time.Instant,

  @get:JsonProperty("reason", required = true) val reason: DepartureReason,

  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("destinationProvider") val destinationProvider: DestinationProvider? = null,
)
