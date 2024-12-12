package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSupportingInformationQuestion
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param assessmentId The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.
 * @param assessmentState 
 * @param dateStarted 
 * @param offenceDetails 
 * @param roshSummary 
 * @param supportingInformation 
 * @param riskToSelf 
 * @param riskManagementPlan 
 * @param dateCompleted 
 */
data class OASysSections(

    @Schema(example = "138985987", required = true, description = "The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.")
    @get:JsonProperty("assessmentId", required = true) val assessmentId: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentState", required = true) val assessmentState: OASysAssessmentState,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dateStarted", required = true) val dateStarted: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("offenceDetails", required = true) val offenceDetails: kotlin.collections.List<OASysQuestion>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roshSummary", required = true) val roshSummary: kotlin.collections.List<OASysQuestion>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("supportingInformation", required = true) val supportingInformation: kotlin.collections.List<OASysSupportingInformationQuestion>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("riskToSelf", required = true) val riskToSelf: kotlin.collections.List<OASysQuestion>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("riskManagementPlan", required = true) val riskManagementPlan: kotlin.collections.List<OASysQuestion>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateCompleted") val dateCompleted: java.time.Instant? = null
) {

}

