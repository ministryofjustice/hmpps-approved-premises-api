package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OASysSupportingInformationQuestion(

  @get:JsonProperty("label", required = true) val label: String,

  @get:JsonProperty("questionNumber", required = true) val questionNumber: String,

  @get:JsonProperty("sectionNumber") val sectionNumber: Int? = null,

  @get:JsonProperty("linkedToHarm") val linkedToHarm: Boolean? = null,

  @get:JsonProperty("linkedToReOffending") val linkedToReOffending: Boolean? = null,

  @get:JsonProperty("answer") val answer: String? = null,
)
