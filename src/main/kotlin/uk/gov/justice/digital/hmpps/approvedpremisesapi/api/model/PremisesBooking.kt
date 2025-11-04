package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PremisesBooking(

  @get:JsonProperty("id") val id: java.util.UUID? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("departureDate") val departureDate: java.time.LocalDate? = null,

  @get:JsonProperty("person") val person: Person? = null,

  @get:JsonProperty("bed") val bed: Bed? = null,

  @get:JsonProperty("status") val status: BookingStatus? = null,
)
