package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: completed,incomplete
*/
enum class OASysAssessmentState(@get:JsonValue val value: kotlin.String) {

  completed("Completed"),
  incomplete("Incomplete"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): OASysAssessmentState = values().first { it -> it.value == value }
  }
}
