package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId The UUID of an application for an AP place
 * @param previousStatus 
 * @param updatedStatus 
 */
data class ApplicationExpired(

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("previousStatus", required = true) val previousStatus: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedStatus", required = true) val updatedStatus: kotlin.String
) {

}

