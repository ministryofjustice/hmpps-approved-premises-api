package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: fullPerson,restrictedPerson,unknownPerson
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PersonType(@get:JsonValue val value: kotlin.String) {

  fullPerson("FullPerson"),
  restrictedPerson("RestrictedPerson"),
  unknownPerson("UnknownPerson"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PersonType = values().first { it -> it.value == value }
  }
}
