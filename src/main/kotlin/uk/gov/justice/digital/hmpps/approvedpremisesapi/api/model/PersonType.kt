package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: fullPerson,restrictedPerson,unknownPerson
*/
enum class PersonType(@get:JsonValue val value: kotlin.String) {

    fullPerson("FullPerson"),
    restrictedPerson("RestrictedPerson"),
    unknownPerson("UnknownPerson");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PersonType {
                return values().first{it -> it.value == value}
        }
    }
}

