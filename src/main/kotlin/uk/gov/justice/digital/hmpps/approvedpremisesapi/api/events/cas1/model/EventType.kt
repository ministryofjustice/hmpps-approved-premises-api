package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* The type of an event
* Values: applicationSubmitted,applicationAssessed,bookingMade,personArrived,personNotArrived,personDeparted,bookingNotMade,bookingCancelled,bookingChanged,bookingKeyWorkerAssigned,applicationWithdrawn,applicationExpired,assessmentAppealed,assessmentAllocated,informationRequestMade,placementApplicationWithdrawn,placementApplicationAllocated,matchRequestWithdrawn,requestForPlacementCreated,requestForPlacementAssessed
*/
enum class EventType(@get:JsonValue val value: kotlin.String) {

  applicationSubmitted("approved-premises.application.submitted"),
  applicationAssessed("approved-premises.application.assessed"),
  bookingMade("approved-premises.booking.made"),
  personArrived("approved-premises.person.arrived"),
  personNotArrived("approved-premises.person.not-arrived"),
  personDeparted("approved-premises.person.departed"),
  bookingNotMade("approved-premises.booking.not-made"),
  bookingCancelled("approved-premises.booking.cancelled"),
  bookingChanged("approved-premises.booking.changed"),
  bookingKeyWorkerAssigned("approved-premises.booking.keyworker.assigned"),
  applicationWithdrawn("approved-premises.application.withdrawn"),
  applicationExpired("approved-premises.application.expired"),
  assessmentAppealed("approved-premises.assessment.appealed"),
  assessmentAllocated("approved-premises.assessment.allocated"),
  informationRequestMade("approved-premises.assessment.info-requested"),
  placementAppealCreated("approved-premises.placement-appeal.created"),
  placementAppealAccepted("approved-premises.placement-appeal.accepted"),
  placementApplicationWithdrawn("approved-premises.placement-application.withdrawn"),
  placementApplicationAllocated("approved-premises.placement-application.allocated"),
  matchRequestWithdrawn("approved-premises.match-request.withdrawn"),
  requestForPlacementCreated("approved-premises.request-for-placement.created"),
  requestForPlacementAssessed("approved-premises.request-for-placement.assessed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): EventType = values().first { it -> it.value == value }
  }
}
