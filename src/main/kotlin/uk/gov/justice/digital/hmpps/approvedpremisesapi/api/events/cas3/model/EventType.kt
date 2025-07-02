package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* The type of an event
* Values: bookingCancelled,bookingCancelledUpdated,bookingConfirmed,bookingProvisionallyMade,personArrived,personArrivedUpdated,personDeparted,referralSubmitted,personDepartureUpdated,assessmentUpdated,draftReferralDeleted
*/
enum class EventType(@get:JsonValue val value: kotlin.String) {

    bookingCancelled("accommodation.cas3.booking.cancelled"),
    bookingCancelledUpdated("accommodation.cas3.booking.cancelled.updated"),
    bookingConfirmed("accommodation.cas3.booking.confirmed"),
    bookingProvisionallyMade("accommodation.cas3.booking.provisionally-made"),
    personArrived("accommodation.cas3.person.arrived"),
    personArrivedUpdated("accommodation.cas3.person.arrived.updated"),
    personDeparted("accommodation.cas3.person.departed"),
    referralSubmitted("accommodation.cas3.referral.submitted"),
    personDepartureUpdated("accommodation.cas3.person.departed.updated"),
    assessmentUpdated("accommodation.cas3.assessment.updated"),
    draftReferralDeleted("accommodation.cas3.draft.referral.deleted");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): EventType {
                return values().first{it -> it.value == value}
        }
    }
}

