package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: riskManagement,residencyManagement,bailAssessment,bailSentence,awaitingSentence
*/
enum class SituationOption(val value: kotlin.String) {

  @JsonProperty("riskManagement")
  riskManagement("riskManagement"),

  @JsonProperty("residencyManagement")
  residencyManagement("residencyManagement"),

  @JsonProperty("bailAssessment")
  bailAssessment("bailAssessment"),

  @JsonProperty("bailSentence")
  bailSentence("bailSentence"),

  @JsonProperty("awaitingSentence")
  awaitingSentence("awaitingSentence"),
}
