package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Notes added to an application
 * @param email 
 * @param name 
 * @param body 
 * @param createdAt 
 * @param id 
 */
data class Cas2ApplicationNote(

    @Schema(example = "roger@example.com", required = true, description = "")
    @get:JsonProperty("email", required = true) val email: kotlin.String,

    @Schema(example = "Roger Smith", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("body", required = true) val body: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: java.util.UUID? = null
) {

}

