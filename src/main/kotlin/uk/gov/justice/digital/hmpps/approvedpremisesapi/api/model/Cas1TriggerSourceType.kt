package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: user,system
*/
enum class Cas1TriggerSourceType(@get:JsonValue val value: kotlin.String) {

  user("user"),
  system("system"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1TriggerSourceType = entries.first { it.value == value }
  }
}
