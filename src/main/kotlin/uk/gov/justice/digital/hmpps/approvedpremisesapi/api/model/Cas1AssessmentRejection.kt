package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1AssessmentRejection(

  @get:JsonProperty("document", required = true) val document: Any,

  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: String,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: String? = null,
)
