package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: applicationSubmitted,applicationAssessed,bookingMade,personArrived,personNotArrived,personDeparted,bookingNotMade,bookingCancelled,bookingChanged,bookingKeyworkerAssigned,applicationWithdrawn,applicationExpired,informationRequest,assessmentAppealed,assessmentAllocated,placementChangeRequestCreated,placementChangeRequestRejected,placementApplicationWithdrawn,placementApplicationAllocated,matchRequestWithdrawn,requestForPlacementCreated,requestForPlacementAssessed,applicationTimelineNote
*/
enum class Cas1TimelineEventType(@get:JsonValue val value: kotlin.String) {

  applicationSubmitted("application_submitted"),
  applicationAssessed("application_assessed"),
  bookingMade("booking_made"),
  personArrived("person_arrived"),
  personNotArrived("person_not_arrived"),
  personDeparted("person_departed"),
  bookingNotMade("booking_not_made"),
  bookingCancelled("booking_cancelled"),
  bookingChanged("booking_changed"),
  bookingKeyworkerAssigned("booking_keyworker_assigned"),
  applicationWithdrawn("application_withdrawn"),
  applicationExpired("application_expired"),
  informationRequest("information_request"),
  assessmentAppealed("assessment_appealed"),
  assessmentAllocated("assessment_allocated"),
  placementChangeRequestCreated("placement_change_request_created"),
  placementChangeRequestRejected("placement_change_request_rejected"),
  placementApplicationWithdrawn("placement_application_withdrawn"),
  placementApplicationAllocated("placement_application_allocated"),
  matchRequestWithdrawn("match_request_withdrawn"),
  requestForPlacementCreated("request_for_placement_created"),
  requestForPlacementAssessed("request_for_placement_assessed"),
  applicationTimelineNote("application_timeline_note"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1TimelineEventType = values().first { it -> it.value == value }
  }
}
