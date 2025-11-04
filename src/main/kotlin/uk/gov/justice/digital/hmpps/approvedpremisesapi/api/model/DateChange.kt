package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class DateChange(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("previousArrivalDate", required = true) val previousArrivalDate: java.time.LocalDate,

  @get:JsonProperty("newArrivalDate", required = true) val newArrivalDate: java.time.LocalDate,

  @get:JsonProperty("previousDepartureDate", required = true) val previousDepartureDate: java.time.LocalDate,

  @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: java.time.LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,
)
