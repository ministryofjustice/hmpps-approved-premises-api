package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: pending,active,archived
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PropertyStatus(@get:JsonValue val value: kotlin.String) {

  pending("pending"),
  active("active"),
  archived("archived"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PropertyStatus = values().first { it -> it.value == value }
  }
}
