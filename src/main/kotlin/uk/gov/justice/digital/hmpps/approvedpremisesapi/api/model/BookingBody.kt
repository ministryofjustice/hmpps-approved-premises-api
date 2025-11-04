package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BookingBody(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: java.time.LocalDate,

  @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

  @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: java.time.LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("serviceName", required = true) val serviceName: ServiceName,

  @get:JsonProperty("bed") val bed: Bed? = null,
)
