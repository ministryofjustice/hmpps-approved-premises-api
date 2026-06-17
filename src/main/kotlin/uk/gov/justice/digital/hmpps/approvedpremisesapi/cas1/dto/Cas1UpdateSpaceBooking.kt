package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Cas1UpdateSpaceBooking(

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", description = "Only provided if the arrival date has changed")
  @get:JsonProperty("arrivalDate") val arrivalDate: LocalDate? = null,

  @Schema(example = "Fri Sep 30 01:00:00 BST 2022", description = "Only provided if the departure date has changed")
  @get:JsonProperty("departureDate") val departureDate: LocalDate? = null,

  @Schema(example = "null", description = "Only provided if characteristics have changed")
  @get:JsonProperty("characteristics") val characteristics: List<Cas1SpaceBookingCharacteristic>? = null,
)
