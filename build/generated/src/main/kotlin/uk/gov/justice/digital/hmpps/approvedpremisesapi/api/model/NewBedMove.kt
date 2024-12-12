package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param bedId 
 * @param notes 
 */
data class NewBedMove(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

