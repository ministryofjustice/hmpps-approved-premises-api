package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1TimelineEventUrlType(@get:JsonValue val value: String) {

  application("application"),
  assessment("assessment"),
  assessmentAppeal("assessmentAppeal"),
  spaceBooking("spaceBooking"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1TimelineEventUrlType = values().first { it -> it.value == value }
  }
}
