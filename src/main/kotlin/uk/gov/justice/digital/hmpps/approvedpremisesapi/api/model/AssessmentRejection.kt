package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param document Any object that conforms to the current JSON schema for an application
 * @param rejectionRationale
 * @param referralRejectionReasonId
 * @param referralRejectionReasonDetail
 * @param isWithdrawn
 */
data class AssessmentRejection(

  @Schema(example = "null", required = true, description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("referralRejectionReasonId") val referralRejectionReasonId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("referralRejectionReasonDetail") val referralRejectionReasonDetail: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isWithdrawn") val isWithdrawn: kotlin.Boolean? = null,
)
