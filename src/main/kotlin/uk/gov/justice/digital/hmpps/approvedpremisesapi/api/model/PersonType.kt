package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PersonType(@get:JsonValue val value: String) {

  fullPerson("FullPerson"),
  restrictedPerson("RestrictedPerson"),
  unknownPerson("UnknownPerson"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): PersonType = values().first { it -> it.value == value }
  }
}
