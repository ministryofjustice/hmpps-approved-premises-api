package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param deliusEventNumber 
 * @param offenceDescription 
 * @param offenceId 
 * @param convictionId 
 * @param offenceDate 
 */
data class ActiveOffence(

    @Schema(example = "7", required = true, description = "")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("offenceDescription", required = true) val offenceDescription: kotlin.String,

    @Schema(example = "M1502750438", required = true, description = "")
    @get:JsonProperty("offenceId", required = true) val offenceId: kotlin.String,

    @Schema(example = "1502724704", required = true, description = "")
    @get:JsonProperty("convictionId", required = true) val convictionId: kotlin.Long,

    @Schema(example = "null", description = "")
    @get:JsonProperty("offenceDate") val offenceDate: java.time.LocalDate? = null
) {

}

