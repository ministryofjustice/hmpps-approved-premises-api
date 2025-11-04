package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Arrival(

  @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: java.time.LocalDate,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @get:JsonProperty("arrivalTime", required = true) val arrivalTime: kotlin.String,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("keyWorkerStaffCode") val keyWorkerStaffCode: kotlin.String? = null,
)
