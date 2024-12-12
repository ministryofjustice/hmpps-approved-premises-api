package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 * @param code 
 * @param bedEndDate End date of the bed availability, open for availability if not specified
 */
data class Bed(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "NEABC04", description = "")
    @get:JsonProperty("code") val code: kotlin.String? = null,

    @Schema(example = "Sat Mar 30 00:00:00 GMT 2024", description = "End date of the bed availability, open for availability if not specified")
    @get:JsonProperty("bedEndDate") val bedEndDate: java.time.LocalDate? = null
) {

}

