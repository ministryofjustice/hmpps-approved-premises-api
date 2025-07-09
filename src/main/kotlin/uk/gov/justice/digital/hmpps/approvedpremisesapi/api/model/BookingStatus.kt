package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: arrived,awaitingMinusArrival,notMinusArrived,departed,cancelled,provisional,confirmed,closed
*/
enum class BookingStatus(@get:JsonValue val value: kotlin.String) {

    arrived("arrived"),
    awaitingMinusArrival("awaiting-arrival"),
    notMinusArrived("not-arrived"),
    departed("departed"),
    cancelled("cancelled"),
    provisional("provisional"),
    confirmed("confirmed"),
    closed("closed");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): BookingStatus {
                return values().first{it -> it.value == value}
        }
    }
}

