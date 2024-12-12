package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param arrivalDateTime This is deprecated. Instead use arrivalDate and arrivalTime
 * @param arrivalDate 
 * @param arrivalTime 
 */
data class Cas1NewArrival(

    @Schema(example = "null", description = "This is deprecated. Instead use arrivalDate and arrivalTime")
    @Deprecated(message = "")
    @get:JsonProperty("arrivalDateTime") val arrivalDateTime: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

    @Schema(example = "23:15", description = "")
    @get:JsonProperty("arrivalTime") val arrivalTime: kotlin.String? = null
) {

}

