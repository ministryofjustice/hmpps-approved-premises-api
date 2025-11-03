package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param document Any object
 * @param rejectionRationale
 * @param agreeWithShortNoticeReason
 * @param agreeWithShortNoticeReasonComments
 * @param reasonForLateApplication
 */
data class Cas1AssessmentRejection(

  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: kotlin.String,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: kotlin.String? = null,
)
