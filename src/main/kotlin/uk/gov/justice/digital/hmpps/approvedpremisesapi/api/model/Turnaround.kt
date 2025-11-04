package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Turnaround(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("workingDays", required = true) val workingDays: kotlin.Int,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,
)
