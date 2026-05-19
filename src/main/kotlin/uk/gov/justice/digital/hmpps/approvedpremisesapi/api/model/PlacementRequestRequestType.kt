package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementRequestRequestType(@get:JsonValue val value: String) {

  parole("parole"),
  standardRelease("standardRelease"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): PlacementRequestRequestType = values().first { it.value == value }
  }
}
