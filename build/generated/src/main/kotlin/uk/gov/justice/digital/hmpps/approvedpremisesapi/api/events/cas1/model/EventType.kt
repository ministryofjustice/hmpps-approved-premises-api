package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* The type of an event
* Values: applicationSubmitted,applicationAssessed,bookingMade,personArrived,personNotArrived,personDeparted,bookingNotMade,bookingCancelled,bookingChanged,bookingKeyWorkerAssigned,applicationWithdrawn,applicationExpired,assessmentAppealed,assessmentAllocated,informationRequestMade,placementApplicationWithdrawn,placementApplicationAllocated,matchRequestWithdrawn,requestForPlacementCreated,requestForPlacementAssessed
*/
enum class EventType(val value: kotlin.String) {

    @JsonProperty("approved-premises.application.submitted") applicationSubmitted("approved-premises.application.submitted"),
    @JsonProperty("approved-premises.application.assessed") applicationAssessed("approved-premises.application.assessed"),
    @JsonProperty("approved-premises.booking.made") bookingMade("approved-premises.booking.made"),
    @JsonProperty("approved-premises.person.arrived") personArrived("approved-premises.person.arrived"),
    @JsonProperty("approved-premises.person.not-arrived") personNotArrived("approved-premises.person.not-arrived"),
    @JsonProperty("approved-premises.person.departed") personDeparted("approved-premises.person.departed"),
    @JsonProperty("approved-premises.booking.not-made") bookingNotMade("approved-premises.booking.not-made"),
    @JsonProperty("approved-premises.booking.cancelled") bookingCancelled("approved-premises.booking.cancelled"),
    @JsonProperty("approved-premises.booking.changed") bookingChanged("approved-premises.booking.changed"),
    @JsonProperty("approved-premises.booking.keyworker.assigned") bookingKeyWorkerAssigned("approved-premises.booking.keyworker.assigned"),
    @JsonProperty("approved-premises.application.withdrawn") applicationWithdrawn("approved-premises.application.withdrawn"),
    @JsonProperty("approved-premises.application.expired") applicationExpired("approved-premises.application.expired"),
    @JsonProperty("approved-premises.assessment.appealed") assessmentAppealed("approved-premises.assessment.appealed"),
    @JsonProperty("approved-premises.assessment.allocated") assessmentAllocated("approved-premises.assessment.allocated"),
    @JsonProperty("approved-premises.assessment.info-requested") informationRequestMade("approved-premises.assessment.info-requested"),
    @JsonProperty("approved-premises.placement-application.withdrawn") placementApplicationWithdrawn("approved-premises.placement-application.withdrawn"),
    @JsonProperty("approved-premises.placement-application.allocated") placementApplicationAllocated("approved-premises.placement-application.allocated"),
    @JsonProperty("approved-premises.match-request.withdrawn") matchRequestWithdrawn("approved-premises.match-request.withdrawn"),
    @JsonProperty("approved-premises.request-for-placement.created") requestForPlacementCreated("approved-premises.request-for-placement.created"),
    @JsonProperty("approved-premises.request-for-placement.assessed") requestForPlacementAssessed("approved-premises.request-for-placement.assessed")
}

