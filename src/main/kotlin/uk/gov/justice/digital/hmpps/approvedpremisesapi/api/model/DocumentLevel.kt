package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class DocumentLevel(@get:JsonValue val value: String) {

  offender("Offender"),
  conviction("Conviction"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): DocumentLevel = values().first { it -> it.value == value }
  }
}
