package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* The type of an event
* Values: applicationSubmitted,applicationStatusUpdated
*/
enum class EventType(@get:JsonValue val value: kotlin.String) {

  applicationSubmitted("applications.cas2.application.submitted"),
  applicationStatusUpdated("applications.cas2.application.status-updated"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): EventType = values().first { it -> it.value == value }
  }
}
