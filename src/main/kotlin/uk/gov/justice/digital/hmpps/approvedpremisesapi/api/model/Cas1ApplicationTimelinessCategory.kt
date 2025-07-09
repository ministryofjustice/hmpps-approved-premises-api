package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: standard,emergency,shortNotice
*/
enum class Cas1ApplicationTimelinessCategory(@get:JsonValue val value: kotlin.String) {

    standard("standard"),
    emergency("emergency"),
    shortNotice("shortNotice");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1ApplicationTimelinessCategory {
                return values().first{it -> it.value == value}
        }
    }
}

