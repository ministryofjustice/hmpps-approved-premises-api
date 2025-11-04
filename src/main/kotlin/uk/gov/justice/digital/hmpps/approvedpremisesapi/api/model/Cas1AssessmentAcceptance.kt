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
data class Cas1AssessmentAcceptance(

  val document: kotlin.Any,

  val requirements: PlacementRequirements,

  val placementDates: PlacementDates? = null,

  val apType: ApType? = null,

  val notes: kotlin.String? = null,

  val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  val reasonForLateApplication: kotlin.String? = null,
)
