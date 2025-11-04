package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementApplicationType(@get:JsonValue val value: kotlin.String) {

  initial("Initial"),
  additional("Additional"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementApplicationType = values().first { it -> it.value == value }
  }
}
