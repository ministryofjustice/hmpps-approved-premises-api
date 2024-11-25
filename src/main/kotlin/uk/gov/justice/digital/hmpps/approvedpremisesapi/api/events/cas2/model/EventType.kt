package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
* The type of an event
* Values: applicationSubmitted,applicationStatusUpdated
*/
enum class EventType(val value: kotlin.String) {

  @JsonProperty("applications.cas2.application.submitted")
  applicationSubmitted("applications.cas2.application.submitted"),

  @JsonProperty("applications.cas2.application.status-updated")
  applicationStatusUpdated("applications.cas2.application.status-updated"),
}
