package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param newStatus The \"name\" of the new status to be applied
 * @param newStatusDetails 
 */
data class Cas2AssessmentStatusUpdate(

    @Schema(example = "moreInfoRequired", required = true, description = "The \"name\" of the new status to be applied")
    @get:JsonProperty("newStatus", required = true) val newStatus: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("newStatusDetails") val newStatusDetails: kotlin.collections.List<kotlin.String>? = null
) {

}

