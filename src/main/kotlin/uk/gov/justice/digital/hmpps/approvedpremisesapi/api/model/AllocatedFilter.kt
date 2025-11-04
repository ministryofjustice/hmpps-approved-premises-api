package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class AllocatedFilter(@get:JsonValue val value: kotlin.String) {

  allocated("allocated"),
  unallocated("unallocated"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): AllocatedFilter = values().first { it -> it.value == value }
  }
}
