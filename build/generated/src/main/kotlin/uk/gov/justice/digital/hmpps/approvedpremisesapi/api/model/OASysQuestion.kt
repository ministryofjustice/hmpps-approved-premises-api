package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param label 
 * @param questionNumber 
 * @param answer 
 */
data class OASysQuestion(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("label", required = true) val label: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("questionNumber", required = true) val questionNumber: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("answer") val answer: kotlin.String? = null
) {

}

