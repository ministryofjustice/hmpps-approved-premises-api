package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param createdByUserId 
 * @param crn 
 * @param nomsNumber 
 * @param personName 
 * @param createdAt 
 * @param submittedAt 
 */
data class Cas2SubmittedApplicationSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("nomsNumber", required = true) val nomsNumber: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personName", required = true) val personName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null
) {

}

