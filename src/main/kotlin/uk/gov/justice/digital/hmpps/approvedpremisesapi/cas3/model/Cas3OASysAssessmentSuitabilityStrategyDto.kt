package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas3OASysAssessmentSuitabilityStrategyDto(@get:JsonValue val value: String) {
  ALLOW_ALL("allow_all"),
  COMPLETED_IN_LAST_SIX_MONTHS("completed_in_last_six_months"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String) = entries.first { it.value == value }
  }
}
