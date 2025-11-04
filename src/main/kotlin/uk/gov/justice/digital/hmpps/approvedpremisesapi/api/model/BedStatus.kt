package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class BedStatus(@get:JsonValue val value: kotlin.String) {

  occupied("occupied"),
  available("available"),
  outOfService("out_of_service"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): BedStatus = values().first { it -> it.value == value }
  }
}
