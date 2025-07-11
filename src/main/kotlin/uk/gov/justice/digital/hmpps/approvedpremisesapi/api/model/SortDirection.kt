package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: asc,desc
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SortDirection(@get:JsonValue val value: kotlin.String) {

  asc("asc"),
  desc("desc"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SortDirection = values().first { it -> it.value == value }
  }
}
