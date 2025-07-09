package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: pending,active,archived
*/
enum class PropertyStatus(@get:JsonValue val value: kotlin.String) {

    pending("pending"),
    active("active"),
    archived("archived");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PropertyStatus {
                return values().first{it -> it.value == value}
        }
    }
}

