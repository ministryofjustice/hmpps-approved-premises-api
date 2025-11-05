package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremiseCapacityForDay(

  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @field:Schema(example = "null", required = true, description = "total bed count including temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
  @get:JsonProperty("totalBedCount", required = true) val totalBedCount: kotlin.Int,

  @field:Schema(example = "null", required = true, description = "total bed count excluding temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
  @get:JsonProperty("availableBedCount", required = true) val availableBedCount: kotlin.Int,

  @field:Schema(example = "null", required = true, description = "total number of bookings in the premise on that day")
  @get:JsonProperty("bookingCount", required = true) val bookingCount: kotlin.Int,

  @get:JsonProperty("characteristicAvailability", required = true) val characteristicAvailability: kotlin.collections.List<Cas1PremiseCharacteristicAvailability>,
)
