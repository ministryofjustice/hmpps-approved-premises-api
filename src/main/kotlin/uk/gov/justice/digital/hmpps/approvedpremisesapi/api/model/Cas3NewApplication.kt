package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param convictionId 
 * @param deliusEventNumber 
 * @param offenceId 
 */
data class Cas3NewApplication(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: String,

    @Schema(example = "1502724704", description = "")
    @get:JsonProperty("convictionId") val convictionId: Long? = null,

    @Schema(example = "7", description = "")
    @get:JsonProperty("deliusEventNumber") val deliusEventNumber: String? = null,

    @Schema(example = "M1502750438", description = "")
    @get:JsonProperty("offenceId") val offenceId: String? = null
    ) {

}

