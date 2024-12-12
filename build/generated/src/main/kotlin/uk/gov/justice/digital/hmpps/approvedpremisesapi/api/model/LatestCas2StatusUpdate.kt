package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param statusId 
 * @param label 
 */
data class LatestCas2StatusUpdate(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("statusId", required = true) val statusId: java.util.UUID,

    @Schema(example = "More information requested", required = true, description = "")
    @get:JsonProperty("label", required = true) val label: kotlin.String
) {

}

