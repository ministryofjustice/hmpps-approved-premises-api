package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param notes 
 */
data class NewConfirmation(

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

