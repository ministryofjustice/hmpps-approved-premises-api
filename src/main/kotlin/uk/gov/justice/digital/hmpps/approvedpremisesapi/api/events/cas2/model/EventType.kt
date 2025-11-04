package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
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
