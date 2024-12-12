package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param addressLine1 
 * @param postcode 
 * @param region 
 * @param addressLine2 
 * @param town 
 */
data class Premises(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("region", required = true) val region: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("town") val town: kotlin.String? = null
) {

}

