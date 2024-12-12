package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param sensitive 
 * @param createdAt 
 * @param occurredAt 
 * @param authorName 
 * @param type 
 * @param subType 
 * @param note 
 */
data class PrisonCaseNote(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("sensitive", required = true) val sensitive: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("authorName", required = true) val authorName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("subType", required = true) val subType: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("note", required = true) val note: kotlin.String
) {

}

