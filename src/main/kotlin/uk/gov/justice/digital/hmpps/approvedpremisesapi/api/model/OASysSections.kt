package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class OASysSections(

  @Schema(example = "138985987", required = true, description = "The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.")
  @get:JsonProperty("assessmentId", required = true) val assessmentId: kotlin.Long,

  @get:JsonProperty("assessmentState", required = true) val assessmentState: OASysAssessmentState,

  @get:JsonProperty("dateStarted", required = true) val dateStarted: java.time.Instant,

  @get:JsonProperty("offenceDetails", required = true) val offenceDetails: kotlin.collections.List<OASysQuestion>,

  @get:JsonProperty("roshSummary", required = true) val roshSummary: kotlin.collections.List<OASysQuestion>,

  @get:JsonProperty("supportingInformation", required = true) val supportingInformation: kotlin.collections.List<OASysSupportingInformationQuestion>,

  @get:JsonProperty("riskToSelf", required = true) val riskToSelf: kotlin.collections.List<OASysQuestion>,

  @get:JsonProperty("riskManagementPlan", required = true) val riskManagementPlan: kotlin.collections.List<OASysQuestion>,

  @get:JsonProperty("dateCompleted") val dateCompleted: java.time.Instant? = null,
)
