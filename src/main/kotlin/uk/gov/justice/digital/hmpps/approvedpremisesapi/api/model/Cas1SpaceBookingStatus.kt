package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas1SpaceBookingStatus(@get:JsonValue val value: String) {
  CANCELLED("cancelled"),
  NOT_ARRIVED("notArrived"),
  DEPARTED("departed"),
  ARRIVED("arrived"),
  UPCOMING("upcoming"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1SpaceBookingStatus = entries.first { it.value == value }
  }
}
