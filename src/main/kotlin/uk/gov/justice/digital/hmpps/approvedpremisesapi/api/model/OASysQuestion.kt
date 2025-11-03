package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param label
 * @param questionNumber
 * @param answer
 */
data class OASysQuestion(

  @get:JsonProperty("label", required = true) val label: kotlin.String,

  @get:JsonProperty("questionNumber", required = true) val questionNumber: kotlin.String,

  @get:JsonProperty("answer") val answer: kotlin.String? = null,
)
