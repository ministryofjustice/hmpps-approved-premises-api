package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param hasApplicableAssessment 
 * @param dateStarted 
 * @param dateCompleted 
 */
data class Cas1OASysAssessmentMetadata(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasApplicableAssessment", required = true) val hasApplicableAssessment: kotlin.Boolean,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateStarted") val dateStarted: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateCompleted") val dateCompleted: java.time.Instant? = null
    ) {

}

