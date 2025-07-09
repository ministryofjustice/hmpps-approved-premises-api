package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param newStatus The \"name\" of the new status to be applied
 * @param newStatusDetails 
 */
data class Cas2v2AssessmentStatusUpdate(

    @Schema(example = "moreInfoRequired", required = true, description = "The \"name\" of the new status to be applied")
    @get:JsonProperty("newStatus", required = true) val newStatus: String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("newStatusDetails") val newStatusDetails: List<String>? = null
    ) {

}

