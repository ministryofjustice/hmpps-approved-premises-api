package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("label", required = true) val label: kotlin.String,

  @get:JsonProperty("questionNumber", required = true) val questionNumber: kotlin.String,

  @get:JsonProperty("sectionNumber") val sectionNumber: kotlin.Int? = null,

  @get:JsonProperty("linkedToHarm") val linkedToHarm: kotlin.Boolean? = null,

  @get:JsonProperty("linkedToReOffending") val linkedToReOffending: kotlin.Boolean? = null,

  @get:JsonProperty("answer") val answer: kotlin.String? = null,
)
