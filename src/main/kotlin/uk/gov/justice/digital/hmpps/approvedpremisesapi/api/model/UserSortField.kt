package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class UserSortField(@get:JsonValue val value: String) {

  personName("name"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): UserSortField = values().first { it -> it.value == value }
  }
}
