package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: assessor,referrer,reporter
*/
enum class TemporaryAccommodationUserRole(@get:JsonValue val value: kotlin.String) {

    assessor("assessor"),
    referrer("referrer"),
    reporter("reporter");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TemporaryAccommodationUserRole {
                return values().first{it -> it.value == value}
        }
    }
}

