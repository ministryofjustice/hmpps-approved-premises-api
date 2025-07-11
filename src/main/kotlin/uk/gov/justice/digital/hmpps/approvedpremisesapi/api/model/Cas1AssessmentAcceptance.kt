package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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

  @Schema(example = "null", required = true, description = "Any object")
  @get:JsonProperty("document", required = true) val document: kotlin.Any,

  @Schema(example = "null", description = "")
  @get:JsonProperty("requirements") val requirements: PlacementRequirements? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("placementDates") val placementDates: PlacementDates? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apType") val apType: ApType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("agreeWithShortNoticeReason") val agreeWithShortNoticeReason: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("agreeWithShortNoticeReasonComments") val agreeWithShortNoticeReasonComments: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reasonForLateApplication") val reasonForLateApplication: kotlin.String? = null,
)
