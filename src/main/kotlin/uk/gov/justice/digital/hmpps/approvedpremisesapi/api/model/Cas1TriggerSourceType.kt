package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: user,system
*/
enum class Cas1TriggerSourceType(@get:JsonValue val value: kotlin.String) {

    user("user"),
    system("system");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1TriggerSourceType {
                return values().first{it -> it.value == value}
        }
    }
}

