package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param document Any object
 * @param rejectionRationale
 * @param referralRejectionReasonId Only used by CAS3
 * @param referralRejectionReasonDetail Only used by CAS3
 * @param isWithdrawn
 * @param agreeWithShortNoticeReason
 * @param agreeWithShortNoticeReasonComments
 * @param reasonForLateApplication
 */
data class AssessmentRejection(

  val document: kotlin.Any,

  val rejectionRationale: kotlin.String,

  @Schema(example = "null", description = "Only used by CAS3")
  val referralRejectionReasonId: java.util.UUID? = null,

  @Schema(example = "null", description = "Only used by CAS3")
  val referralRejectionReasonDetail: kotlin.String? = null,

  val isWithdrawn: kotlin.Boolean? = null,

  val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  val reasonForLateApplication: kotlin.String? = null,
)
