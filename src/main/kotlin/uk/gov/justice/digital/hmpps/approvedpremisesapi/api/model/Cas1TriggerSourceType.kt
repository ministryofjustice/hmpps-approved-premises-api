package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: user,system
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1TriggerSourceType(@get:JsonValue val value: kotlin.String) {

  user("user"),
  system("system"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1TriggerSourceType = values().first { it -> it.value == value }
  }
}
