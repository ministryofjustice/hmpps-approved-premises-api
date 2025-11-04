package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

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
