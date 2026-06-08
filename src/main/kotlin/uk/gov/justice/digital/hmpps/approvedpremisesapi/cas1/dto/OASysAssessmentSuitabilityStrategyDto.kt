package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class OASysAssessmentSuitabilityStrategyDto(@get:JsonValue val value: String) {
  ALLOW_ALL("allow_all"),
  COMPLETED_IN_LAST_SIX_MONTHS("completed_in_last_six_months"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String) = entries.first { it.value == value }
  }
}
