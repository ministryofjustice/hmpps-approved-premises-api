package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: requestUnsubmitted,requestRejected,requestSubmitted,awaitingMatch,requestWithdrawn,placementBooked,personArrived,personNotArrived,personDeparted
*/
enum class RequestForPlacementStatus(@get:JsonValue val value: kotlin.String) {

    requestUnsubmitted("request_unsubmitted"),
    requestRejected("request_rejected"),
    requestSubmitted("request_submitted"),
    awaitingMatch("awaiting_match"),
    requestWithdrawn("request_withdrawn"),
    placementBooked("placement_booked"),
    personArrived("person_arrived"),
    personNotArrived("person_not_arrived"),
    personDeparted("person_departed");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): RequestForPlacementStatus {
                return values().first{it -> it.value == value}
        }
    }
}

