package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param username 
 * @param name 
 * @param email 
 * @param origin 
 */
data class ExternalUser(

    @Schema(example = "CAS2_ASSESSOR_USER", required = true, description = "")
    @get:JsonProperty("username", required = true) val username: kotlin.String,

    @Schema(example = "Roger Smith", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "roger@external.example.com", required = true, description = "")
    @get:JsonProperty("email", required = true) val email: kotlin.String,

    @Schema(example = "NACRO", description = "")
    @get:JsonProperty("origin") val origin: kotlin.String? = null
) {

}

