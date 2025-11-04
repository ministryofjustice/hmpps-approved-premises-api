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

  val document: kotlin.Any,

  val rejectionRationale: kotlin.String,

  val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  val reasonForLateApplication: kotlin.String? = null,
)
