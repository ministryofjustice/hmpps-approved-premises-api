package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremiseCapacityForDay(

  val date: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "total bed count including temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
  val totalBedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "total bed count excluding temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
  val availableBedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "total number of bookings in the premise on that day")
  val bookingCount: kotlin.Int,

  val characteristicAvailability: kotlin.collections.List<Cas1PremiseCharacteristicAvailability>,
)
