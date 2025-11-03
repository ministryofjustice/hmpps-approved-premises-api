package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param document Any object
 * @param requirements
 * @param placementDates
 * @param apType
 * @param notes
 * @param agreeWithShortNoticeReason
 * @param agreeWithShortNoticeReasonComments
 * @param reasonForLateApplication
 */
data class AssessmentAcceptance(

  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @get:JsonProperty("requirements") val requirements: PlacementRequirements? = null,

  @get:JsonProperty("placementDates") val placementDates: PlacementDates? = null,

  @get:JsonProperty("apType") val apType: ApType? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: kotlin.String? = null,
)
