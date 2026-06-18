package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceCategory(@get:JsonValue val value: String) {

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
    fun forValue(value: String): Cas1SpaceCategory = values().first { it.value == value }
  }
}
