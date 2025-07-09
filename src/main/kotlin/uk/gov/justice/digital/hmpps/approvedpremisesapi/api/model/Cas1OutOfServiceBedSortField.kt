package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: premisesName,roomName,bedName,startDate,endDate,reason,daysLost
*/
enum class Cas1OutOfServiceBedSortField(@get:JsonValue val value: kotlin.String) {

    premisesName("premisesName"),
    roomName("roomName"),
    bedName("bedName"),
    startDate("startDate"),
    endDate("endDate"),
    reason("reason"),
    daysLost("daysLost");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1OutOfServiceBedSortField {
                return values().first{it -> it.value == value}
        }
    }
}

