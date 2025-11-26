package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ServiceType(@get:JsonValue val value: String) {

  CAS2("CAS2"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): ServiceType = entries.first { it -> it.value == value }
  }
}
