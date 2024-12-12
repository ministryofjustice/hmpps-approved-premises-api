package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param deliusUsername 
 * @param loadError 
 * @param user 
 */
data class ProfileResponse(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deliusUsername", required = true) val deliusUsername: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("loadError") val loadError: ProfileResponse.LoadError? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("user") val user: User? = null
) {

    /**
    * 
    * Values: staffRecordNotFound
    */
    enum class LoadError(val value: kotlin.String) {

        @JsonProperty("staff_record_not_found") staffRecordNotFound("staff_record_not_found")
    }

}

