package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
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
  val assessmentId: kotlin.Long,

  val assessmentState: OASysAssessmentState,

  val dateStarted: java.time.Instant,

  val offenceDetails: kotlin.collections.List<OASysQuestion>,

  val roshSummary: kotlin.collections.List<OASysQuestion>,

  val supportingInformation: kotlin.collections.List<OASysSupportingInformationQuestion>,

  val riskToSelf: kotlin.collections.List<OASysQuestion>,

  val riskManagementPlan: kotlin.collections.List<OASysQuestion>,

  val dateCompleted: java.time.Instant? = null,
)
