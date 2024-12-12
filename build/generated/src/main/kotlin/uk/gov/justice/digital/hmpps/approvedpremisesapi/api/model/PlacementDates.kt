package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param expectedArrival 
 * @param duration 
 */
data class PlacementDates(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("duration", required = true) val duration: kotlin.Int
) {

}

