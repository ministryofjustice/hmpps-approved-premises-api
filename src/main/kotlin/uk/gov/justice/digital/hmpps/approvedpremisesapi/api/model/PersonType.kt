package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: fullPerson,restrictedPerson,unknownPerson
*/
enum class PersonType(@get:JsonValue val value: kotlin.String) {

  fullPerson("FullPerson"),
  restrictedPerson("RestrictedPerson"),
  unknownPerson("UnknownPerson"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PersonType = entries.first { it.value == value }
  }
}
