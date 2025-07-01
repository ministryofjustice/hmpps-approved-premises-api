package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: arrived,notMinusArrived,departed,cancelled,provisional,confirmed,closed
*/
enum class Cas3BookingStatus(@get:JsonValue val value: String) {

    arrived("arrived"),
    notMinusArrived("notMinusArrived"),
    departed("departed"),
    cancelled("cancelled"),
    provisional("provisional"),
    confirmed("confirmed"),
    closed("closed");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3BookingStatus {
                return values().first{it -> it.value == value}
        }
    }
}

