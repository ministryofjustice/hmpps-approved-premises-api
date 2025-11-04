package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SituationOption(@get:JsonValue val value: kotlin.String) {

  riskManagement("riskManagement"),
  residencyManagement("residencyManagement"),
  bailAssessment("bailAssessment"),
  bailSentence("bailSentence"),
  awaitingSentence("awaitingSentence"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SituationOption = values().first { it -> it.value == value }
  }
}
