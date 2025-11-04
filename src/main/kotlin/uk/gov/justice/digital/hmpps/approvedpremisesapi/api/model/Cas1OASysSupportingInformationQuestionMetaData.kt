package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param section The OAsys section that this question relates to
 * @param sectionLabel
 * @param inclusionOptional If the user can optionally elect to include this question in an application. If not optional, it will always be returned by calls to '/cas1/people/{crn}/oasys/answers'
 * @param oasysAnswerLinkedToHarm If the response to this question in OASys for the person has been identified as 'linked to harm'
 * @param oasysAnswerLinkedToReOffending If the response to this question in OAsys for the person hsa been identified as 'linked to re-offending'
 */
data class Cas1OASysSupportingInformationQuestionMetaData(

  @Schema(example = "10", required = true, description = "The OAsys section that this question relates to")
  val section: kotlin.Int,

  val sectionLabel: kotlin.String,

  @Schema(example = "null", required = true, description = "If the user can optionally elect to include this question in an application. If not optional, it will always be returned by calls to '/cas1/people/{crn}/oasys/answers'")
  val inclusionOptional: kotlin.Boolean,

  @Schema(example = "null", description = "If the response to this question in OASys for the person has been identified as 'linked to harm'")
  val oasysAnswerLinkedToHarm: kotlin.Boolean? = null,

  @Schema(example = "null", description = "If the response to this question in OAsys for the person hsa been identified as 'linked to re-offending'")
  val oasysAnswerLinkedToReOffending: kotlin.Boolean? = null,
)
