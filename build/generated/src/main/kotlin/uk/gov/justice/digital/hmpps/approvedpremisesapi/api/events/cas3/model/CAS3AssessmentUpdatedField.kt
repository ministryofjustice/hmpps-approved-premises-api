package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param fieldName 
 * @param updatedFrom 
 * @param updatedTo 
 */
data class CAS3AssessmentUpdatedField(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("fieldName", required = true) val fieldName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedFrom", required = true) val updatedFrom: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedTo", required = true) val updatedTo: kotlin.String
) {

}

