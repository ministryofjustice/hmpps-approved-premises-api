package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RequestForPlacementStatus(@get:JsonValue val value: kotlin.String) {

  requestUnsubmitted("request_unsubmitted"),
  requestRejected("request_rejected"),
  requestSubmitted("request_submitted"),
  awaitingMatch("awaiting_match"),
  requestWithdrawn("request_withdrawn"),
  placementBooked("placement_booked"),
  personArrived("person_arrived"),
  personNotArrived("person_not_arrived"),
  personDeparted("person_departed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): RequestForPlacementStatus = values().first { it -> it.value == value }
  }
}
