package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class AssessmentRejection(

  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: kotlin.String,

  @Schema(example = "null", description = "Only used by CAS3")
  @get:JsonProperty("referralRejectionReasonId") val referralRejectionReasonId: java.util.UUID? = null,

  @Schema(example = "null", description = "Only used by CAS3")
  @get:JsonProperty("referralRejectionReasonDetail") val referralRejectionReasonDetail: kotlin.String? = null,

  @get:JsonProperty("isWithdrawn") val isWithdrawn: kotlin.Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: kotlin.String? = null,
)
