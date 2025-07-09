package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: upcoming,current,historic
*/
enum class Cas1SpaceBookingResidency(@get:JsonValue val value: kotlin.String) {

    upcoming("upcoming"),
    current("current"),
    historic("historic");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1SpaceBookingResidency {
                return values().first{it -> it.value == value}
        }
    }
}

