package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1AssessmentAcceptance(

  @get:JsonProperty("document", required = true) val document: Any,

  @get:JsonProperty("requirements") val requirements: PlacementRequirements,

  @get:JsonProperty("placementDates") val placementDates: PlacementDates? = null,

  @get:JsonProperty("apType") val apType: ApType? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: String? = null,
)
