package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param assessmentId The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.
 * @param assessmentState 
 * @param dateStarted 
 * @param riskToSelf 
 * @param dateCompleted 
 */
data class OASysRiskToSelf(

    @Schema(example = "138985987", required = true, description = "The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.")
    @get:JsonProperty("assessmentId", required = true) val assessmentId: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentState", required = true) val assessmentState: OASysAssessmentState,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dateStarted", required = true) val dateStarted: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("riskToSelf", required = true) val riskToSelf: kotlin.collections.List<OASysQuestion>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateCompleted") val dateCompleted: java.time.Instant? = null
    ) {

}

