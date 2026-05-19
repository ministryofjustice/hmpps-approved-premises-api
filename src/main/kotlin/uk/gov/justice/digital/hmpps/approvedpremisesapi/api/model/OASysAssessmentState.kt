package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class OASysAssessmentState(@get:JsonValue val value: String) {

  completed("Completed"),
  incomplete("Incomplete"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): OASysAssessmentState = values().first { it.value == value }
  }
}
