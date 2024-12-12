package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param notes 
 * @param withdrawables 
 */
data class Withdrawables(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notes", required = true) val notes: kotlin.collections.List<kotlin.String>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("withdrawables", required = true) val withdrawables: kotlin.collections.List<Withdrawable>
) {

}

