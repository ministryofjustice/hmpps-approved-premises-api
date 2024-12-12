package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 * @param serviceScope 
 * @param isActive 
 */
data class ReferralRejectionReason(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "There was not enough time to place them", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceScope", required = true) val serviceScope: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isActive", required = true) val isActive: kotlin.Boolean
) {

}

