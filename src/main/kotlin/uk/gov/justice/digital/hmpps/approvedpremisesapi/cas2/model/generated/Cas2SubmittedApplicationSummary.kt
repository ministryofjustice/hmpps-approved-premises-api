package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

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
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("nomsNumber", required = true) val nomsNumber: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personName", required = true) val personName: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null
    ) {

}

