package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: past,current,future
*/
enum class Temporality(@get:JsonValue val value: kotlin.String) {

    past("past"),
    current("current"),
    future("future");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Temporality {
                return values().first{it -> it.value == value}
        }
    }
}

