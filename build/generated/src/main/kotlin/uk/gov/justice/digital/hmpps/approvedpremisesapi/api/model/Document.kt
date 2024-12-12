package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Meta Info about a file relating to an Offender
 * @param id 
 * @param level 
 * @param fileName 
 * @param createdAt 
 * @param typeCode 
 * @param typeDescription 
 * @param description 
 */
data class Document(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("level", required = true) val level: DocumentLevel,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("fileName", required = true) val fileName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("typeCode", required = true) val typeCode: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("typeDescription", required = true) val typeDescription: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("description") val description: kotlin.String? = null
) {

}

