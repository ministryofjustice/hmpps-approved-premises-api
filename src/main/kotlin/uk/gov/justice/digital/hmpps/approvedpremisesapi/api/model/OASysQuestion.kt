package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OASysQuestion(

  @get:JsonProperty("label", required = true) val label: String,

  @get:JsonProperty("questionNumber", required = true) val questionNumber: String,

  @get:JsonProperty("answer") val answer: String? = null,
)
