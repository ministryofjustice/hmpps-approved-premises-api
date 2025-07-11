package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: application,booking,assessment,assessmentAppeal,spaceBooking
*/
enum class Cas1TimelineEventUrlType(@get:JsonValue val value: kotlin.String) {

  application("application"),
  booking("booking"),
  assessment("assessment"),
  assessmentAppeal("assessmentAppeal"),
  spaceBooking("spaceBooking"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1TimelineEventUrlType = values().first { it -> it.value == value }
  }
}
