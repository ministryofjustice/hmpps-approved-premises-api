package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: ascending,descending
*/
enum class SortOrder(@get:JsonValue val value: kotlin.String) {

    ascending("ascending"),
    descending("descending");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SortOrder {
                return values().first{it -> it.value == value}
        }
    }
}

