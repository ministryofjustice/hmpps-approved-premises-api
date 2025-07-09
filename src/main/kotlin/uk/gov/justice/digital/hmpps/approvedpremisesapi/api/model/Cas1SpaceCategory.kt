package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* Spaces are categorised by these traits - 'standard' plus selected physcial and risk characteristics
* Values: standard,arson,wheelchair,sexOffender,enSuite,single
*/
enum class Cas1SpaceCategory(@get:JsonValue val value: kotlin.String) {

    standard("standard"),
    arson("arson"),
    wheelchair("wheelchair"),
    sexOffender("sexOffender"),
    enSuite("enSuite"),
    single("single");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1SpaceCategory {
                return values().first{it -> it.value == value}
        }
    }
}

