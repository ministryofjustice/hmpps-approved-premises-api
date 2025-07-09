package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: allocated,unallocated
*/
enum class AllocatedFilter(@get:JsonValue val value: kotlin.String) {

    allocated("allocated"),
    unallocated("unallocated");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): AllocatedFilter {
                return values().first{it -> it.value == value}
        }
    }
}

