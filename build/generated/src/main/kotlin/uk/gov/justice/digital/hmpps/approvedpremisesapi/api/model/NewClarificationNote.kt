package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param query 
 */
data class NewClarificationNote(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("query", required = true) val query: kotlin.String
) {

}

