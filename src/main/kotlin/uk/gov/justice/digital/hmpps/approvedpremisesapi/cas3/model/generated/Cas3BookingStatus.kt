package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas3BookingStatus(@get:JsonValue val value: String) {

  arrived("arrived"),
  notMinusArrived("notMinusArrived"),
  departed("departed"),
  cancelled("cancelled"),
  provisional("provisional"),
  confirmed("confirmed"),
  closed("closed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3BookingStatus = entries.first { it.value == value }
  }
}
