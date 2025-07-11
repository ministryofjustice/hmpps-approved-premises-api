package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: personName
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class UserSortField(@get:JsonValue val value: kotlin.String) {

  personName("name"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): UserSortField = entries.first { it.value == value }
  }
}
