package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: arrived,awaitingMinusArrival,notMinusArrived,departed,cancelled,provisional,confirmed,closed
*/
enum class BookingStatus(@get:JsonValue val value: kotlin.String) {

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
    fun forValue(value: kotlin.String): BookingStatus = entries.first { it.value == value }
  }
}
