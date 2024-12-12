package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 * @param isActive 
 * @param serviceScope 
 */
data class LostBedReason(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "Double Room with Single Occupancy - Other (Non-FM)", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isActive", required = true) val isActive: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceScope", required = true) val serviceScope: kotlin.String
) {

}

