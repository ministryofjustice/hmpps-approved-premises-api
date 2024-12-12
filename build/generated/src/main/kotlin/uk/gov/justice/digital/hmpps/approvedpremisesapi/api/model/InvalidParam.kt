package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param propertyName 
 * @param errorType 
 */
data class InvalidParam(

    @Schema(example = "arrivalDate", description = "")
    @get:JsonProperty("propertyName") val propertyName: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("errorType") val errorType: kotlin.String? = null
) {

}

