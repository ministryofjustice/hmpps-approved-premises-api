package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: parole,standardRelease
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementRequestRequestType(@get:JsonValue val value: kotlin.String) {

  parole("parole"),
  standardRelease("standardRelease"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementRequestRequestType = entries.first { it.value == value }
  }
}
