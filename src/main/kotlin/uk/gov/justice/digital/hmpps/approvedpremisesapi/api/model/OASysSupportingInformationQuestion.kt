package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param label
 * @param questionNumber
 * @param sectionNumber
 * @param linkedToHarm
 * @param linkedToReOffending
 * @param answer
 */
data class OASysSupportingInformationQuestion(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("questionNumber", required = true) val questionNumber: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("sectionNumber") val sectionNumber: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("linkedToHarm") val linkedToHarm: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("linkedToReOffending") val linkedToReOffending: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("answer") val answer: kotlin.String? = null,
)
