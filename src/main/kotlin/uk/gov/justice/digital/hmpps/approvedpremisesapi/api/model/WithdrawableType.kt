package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: application,booking,placementApplication,placementRequest,spaceBooking
*/
enum class WithdrawableType(@get:JsonValue val value: kotlin.String) {

    application("application"),
    booking("booking"),
    placementApplication("placement_application"),
    placementRequest("placement_request"),
    spaceBooking("space_booking");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): WithdrawableType {
                return values().first{it -> it.value == value}
        }
    }
}

