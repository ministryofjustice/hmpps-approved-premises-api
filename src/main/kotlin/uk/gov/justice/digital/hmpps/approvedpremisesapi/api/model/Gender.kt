package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: male,female
*/
enum class Gender(@get:JsonValue val value: kotlin.String) {

  male("male"),
  female("female"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Gender = entries.first { it.value == value }
  }
}
