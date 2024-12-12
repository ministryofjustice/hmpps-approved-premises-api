package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristicAvailability
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param date 
 * @param totalBedCount total bed count including temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.
 * @param availableBedCount total bed count excluding temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.
 * @param bookingCount total number of bookings in the premise on that day
 * @param characteristicAvailability 
 */
data class Cas1PremiseCapacityForDay(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "total bed count including temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
    @get:JsonProperty("totalBedCount", required = true) val totalBedCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "total bed count excluding temporarily unavailable beds (e.g. out of service beds). this does not consider bookings.")
    @get:JsonProperty("availableBedCount", required = true) val availableBedCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "total number of bookings in the premise on that day")
    @get:JsonProperty("bookingCount", required = true) val bookingCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("characteristicAvailability", required = true) val characteristicAvailability: kotlin.collections.List<Cas1PremiseCharacteristicAvailability>
) {

}

