package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1AssessmentRejection(

  @get:JsonProperty("document", required = true) val document: Any,

  @get:Schema(description = "A human readable description of the reason the assessment was rejected. This is free text.")
  @get:JsonProperty("rejectionRationale", required = true) val rejectionRationale: String,

  @get:Schema(description = "An enumeration for the reason the assessment was rejected. This can reliably drive behaviour, such as which email is sent when an application is rejected.")
  @get:JsonProperty("rejectionReason", required = true) val rejectionReason: Cas1AssessmentRejectionReasonDto,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: String? = null,
)
