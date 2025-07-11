package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param document Any object
 * @param rejectionRationale
 * @param agreeWithShortNoticeReason
 * @param agreeWithShortNoticeReasonComments
 * @param reasonForLateApplication
 */
data class Cas1AssessmentRejection(

  @Schema(example = "null", required = true, description = "Any object")
  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: kotlin.String? = null,
)
