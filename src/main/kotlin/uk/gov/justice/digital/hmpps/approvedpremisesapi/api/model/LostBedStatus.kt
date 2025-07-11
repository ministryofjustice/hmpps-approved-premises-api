package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: active,cancelled
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class LostBedStatus(@get:JsonValue val value: kotlin.String) {

  active("active"),
  cancelled("cancelled"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): LostBedStatus = values().first { it -> it.value == value }
  }
}
