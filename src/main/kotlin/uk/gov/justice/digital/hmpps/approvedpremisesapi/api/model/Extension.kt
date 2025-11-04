package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Extension(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("previousDepartureDate", required = true) val previousDepartureDate: java.time.LocalDate,

  @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: java.time.LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
