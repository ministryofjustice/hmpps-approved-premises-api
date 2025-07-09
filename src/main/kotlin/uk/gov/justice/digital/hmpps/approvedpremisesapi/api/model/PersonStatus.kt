package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: inCustody,inCommunity,unknown
*/
enum class PersonStatus(@get:JsonValue val value: kotlin.String) {

    inCustody("InCustody"),
    inCommunity("InCommunity"),
    unknown("Unknown");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PersonStatus {
                return values().first{it -> it.value == value}
        }
    }
}

