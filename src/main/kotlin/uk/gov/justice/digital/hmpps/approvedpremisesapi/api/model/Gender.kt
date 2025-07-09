package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: male,female
*/
enum class Gender(@get:JsonValue val value: kotlin.String) {

    male("male"),
    female("female");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Gender {
                return values().first{it -> it.value == value}
        }
    }
}

