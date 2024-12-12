package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param reasonId 
 * @param departureDateTime 
 * @param departureDate 
 * @param departureTime 
 * @param moveOnCategoryId 
 * @param notes 
 */
data class Cas1NewDeparture(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("departureDateTime") val departureDateTime: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("departureDate") val departureDate: java.time.LocalDate? = null,

    @Schema(example = "23:15", description = "")
    @get:JsonProperty("departureTime") val departureTime: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("moveOnCategoryId") val moveOnCategoryId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

