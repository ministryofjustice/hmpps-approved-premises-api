package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param totalBedCount total bed count including temporarily unavailable beds (e.g. out of service beds)
 * @param availableBedCount total bed count excluding temporarily unavailable beds (e.g. out of service beds)
 * @param bookingCount
 * @param characteristicAvailability
 */
data class Cas1PremiseCapacityForDay(

  @Schema(example = "null", required = true, description = "total bed count including temporarily unavailable beds (e.g. out of service beds)")
  @get:JsonProperty("totalBedCount", required = true) val totalBedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "total bed count excluding temporarily unavailable beds (e.g. out of service beds)")
  @get:JsonProperty("availableBedCount", required = true) val availableBedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingCount", required = true) val bookingCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristicAvailability", required = true) val characteristicAvailability: kotlin.collections.List<Cas1PremiseCharacteristicAvailability>,
)
