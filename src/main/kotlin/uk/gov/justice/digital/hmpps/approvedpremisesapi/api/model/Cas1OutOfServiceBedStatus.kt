package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: active,cancelled
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1OutOfServiceBedStatus(@get:JsonValue val value: kotlin.String) {

  active("active"),
  cancelled("cancelled"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1OutOfServiceBedStatus = values().first { it -> it.value == value }
  }
}
