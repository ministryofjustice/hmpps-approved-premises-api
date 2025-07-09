package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: tier,createdAt,arrivalDate,releaseType
*/
enum class ApplicationSortField(@get:JsonValue val value: kotlin.String) {

    tier("tier"),
    createdAt("createdAt"),
    arrivalDate("arrivalDate"),
    releaseType("releaseType");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApplicationSortField {
                return values().first{it -> it.value == value}
        }
    }
}

