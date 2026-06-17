package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremiseCharacteristicAvailability(

  @get:JsonProperty("characteristic", required = true) val characteristic: Cas1SpaceBookingCharacteristic,

  @Schema(example = "null", required = true, description = "the number of available beds with this characteristic")
  @get:JsonProperty("availableBedsCount", required = true) val availableBedsCount: Int,

  @Schema(example = "null", required = true, description = "the number of bookings requiring this characteristic")
  @get:JsonProperty("bookingsCount", required = true) val bookingsCount: Int,
)
