package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1UpdateSpaceBooking(

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", description = "Only provided if the arrival date has changed")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @Schema(example = "Fri Sep 30 01:00:00 BST 2022", description = "Only provided if the departure date has changed")
  @get:JsonProperty("departureDate") val departureDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Only provided if characteristics have changed")
  @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceBookingCharacteristic>? = null,
)
