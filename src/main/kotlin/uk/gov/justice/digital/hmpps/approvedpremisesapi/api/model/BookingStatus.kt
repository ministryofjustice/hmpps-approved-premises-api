package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class BookingStatus(@get:JsonValue val value: String) {

  arrived("arrived"),
  awaitingMinusArrival("awaiting-arrival"),
  notMinusArrived("not-arrived"),
  departed("departed"),
  cancelled("cancelled"),
  provisional("provisional"),
  confirmed("confirmed"),
  closed("closed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): BookingStatus = values().first { it.value == value }
  }
}
