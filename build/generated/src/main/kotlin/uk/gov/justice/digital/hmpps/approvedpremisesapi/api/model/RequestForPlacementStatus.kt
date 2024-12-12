package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: requestUnsubmitted,requestRejected,requestSubmitted,awaitingMatch,requestWithdrawn,placementBooked,personArrived,personNotArrived,personDeparted
*/
enum class RequestForPlacementStatus(val value: kotlin.String) {

    @JsonProperty("request_unsubmitted") requestUnsubmitted("request_unsubmitted"),
    @JsonProperty("request_rejected") requestRejected("request_rejected"),
    @JsonProperty("request_submitted") requestSubmitted("request_submitted"),
    @JsonProperty("awaiting_match") awaitingMatch("awaiting_match"),
    @JsonProperty("request_withdrawn") requestWithdrawn("request_withdrawn"),
    @JsonProperty("placement_booked") placementBooked("placement_booked"),
    @JsonProperty("person_arrived") personArrived("person_arrived"),
    @JsonProperty("person_not_arrived") personNotArrived("person_not_arrived"),
    @JsonProperty("person_departed") personDeparted("person_departed")
}

