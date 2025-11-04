package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceCategory(@get:JsonValue val value: kotlin.String) {

  standard("standard"),
  arson("arson"),
  wheelchair("wheelchair"),
  sexOffender("sexOffender"),
  enSuite("enSuite"),
  single("single"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceCategory = values().first { it -> it.value == value }
  }
}
