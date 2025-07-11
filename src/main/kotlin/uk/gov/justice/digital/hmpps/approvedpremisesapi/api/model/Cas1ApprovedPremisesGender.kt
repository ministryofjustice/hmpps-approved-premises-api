package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: man,woman
*/
enum class Cas1ApprovedPremisesGender(@get:JsonValue val value: kotlin.String) {

  man("man"),
  woman("woman"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ApprovedPremisesGender = values().first { it -> it.value == value }
  }
}
