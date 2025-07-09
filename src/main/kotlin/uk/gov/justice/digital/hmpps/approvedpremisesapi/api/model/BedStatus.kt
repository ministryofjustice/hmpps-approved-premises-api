package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: occupied,available,outOfService
*/
enum class BedStatus(@get:JsonValue val value: kotlin.String) {

    occupied("occupied"),
    available("available"),
    outOfService("out_of_service");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): BedStatus {
                return values().first{it -> it.value == value}
        }
    }
}

