package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: ascending,descending
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SortOrder(@get:JsonValue val value: kotlin.String) {

  ascending("ascending"),
  descending("descending"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SortOrder = values().first { it -> it.value == value }
  }
}
