package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: approvedPremisesApplicationSubmitted,approvedPremisesApplicationAssessed,approvedPremisesBookingMade,approvedPremisesPersonArrived,approvedPremisesPersonNotArrived,approvedPremisesPersonDeparted,approvedPremisesBookingNotMade,approvedPremisesBookingCancelled,approvedPremisesBookingChanged,approvedPremisesBookingKeyworkerAssigned,approvedPremisesApplicationWithdrawn,approvedPremisesApplicationExpired,approvedPremisesInformationRequest,approvedPremisesAssessmentAppealed,approvedPremisesAssessmentAllocated,approvedPremisesPlacementApplicationWithdrawn,approvedPremisesPlacementApplicationAllocated,approvedPremisesMatchRequestWithdrawn,approvedPremisesRequestForPlacementCreated,approvedPremisesRequestForPlacementAssessed,cas3PersonArrived,cas3PersonDeparted,applicationTimelineNote,cas2ApplicationSubmitted,cas2Note,cas2StatusUpdate
*/
enum class TimelineEventType(val value: kotlin.String) {

  @JsonProperty("approved_premises_application_submitted")
  approvedPremisesApplicationSubmitted("approved_premises_application_submitted"),

  @JsonProperty("approved_premises_application_assessed")
  approvedPremisesApplicationAssessed("approved_premises_application_assessed"),

  @JsonProperty("approved_premises_booking_made")
  approvedPremisesBookingMade("approved_premises_booking_made"),

  @JsonProperty("approved_premises_person_arrived")
  approvedPremisesPersonArrived("approved_premises_person_arrived"),

  @JsonProperty("approved_premises_person_not_arrived")
  approvedPremisesPersonNotArrived("approved_premises_person_not_arrived"),

  @JsonProperty("approved_premises_person_departed")
  approvedPremisesPersonDeparted("approved_premises_person_departed"),

  @JsonProperty("approved_premises_booking_not_made")
  approvedPremisesBookingNotMade("approved_premises_booking_not_made"),

  @JsonProperty("approved_premises_booking_cancelled")
  approvedPremisesBookingCancelled("approved_premises_booking_cancelled"),

  @JsonProperty("approved_premises_booking_changed")
  approvedPremisesBookingChanged("approved_premises_booking_changed"),

  @JsonProperty("approved_premises_booking_keyworker_assigned")
  approvedPremisesBookingKeyworkerAssigned("approved_premises_booking_keyworker_assigned"),

  @JsonProperty("approved_premises_application_withdrawn")
  approvedPremisesApplicationWithdrawn("approved_premises_application_withdrawn"),

  @JsonProperty("approved_premises_application_expired")
  approvedPremisesApplicationExpired("approved_premises_application_expired"),

  @JsonProperty("approved_premises_information_request")
  approvedPremisesInformationRequest("approved_premises_information_request"),

  @JsonProperty("approved_premises_assessment_appealed")
  approvedPremisesAssessmentAppealed("approved_premises_assessment_appealed"),

  @JsonProperty("approved_premises_assessment_allocated")
  approvedPremisesAssessmentAllocated("approved_premises_assessment_allocated"),

  @JsonProperty("approved_premises_placement_application_withdrawn")
  approvedPremisesPlacementApplicationWithdrawn("approved_premises_placement_application_withdrawn"),

  @JsonProperty("approved_premises_placement_application_allocated")
  approvedPremisesPlacementApplicationAllocated("approved_premises_placement_application_allocated"),

  @JsonProperty("approved_premises_match_request_withdrawn")
  approvedPremisesMatchRequestWithdrawn("approved_premises_match_request_withdrawn"),

  @JsonProperty("approved_premises_request_for_placement_created")
  approvedPremisesRequestForPlacementCreated("approved_premises_request_for_placement_created"),

  @JsonProperty("approved_premises_request_for_placement_assessed")
  approvedPremisesRequestForPlacementAssessed("approved_premises_request_for_placement_assessed"),

  @JsonProperty("cas3_person_arrived")
  cas3PersonArrived("cas3_person_arrived"),

  @JsonProperty("cas3_person_departed")
  cas3PersonDeparted("cas3_person_departed"),

  @JsonProperty("application_timeline_note")
  applicationTimelineNote("application_timeline_note"),

  @JsonProperty("cas2_application_submitted")
  cas2ApplicationSubmitted("cas2_application_submitted"),

  @JsonProperty("cas2_note")
  cas2Note("cas2_note"),

  @JsonProperty("cas2_status_update")
  cas2StatusUpdate("cas2_status_update"),
}
