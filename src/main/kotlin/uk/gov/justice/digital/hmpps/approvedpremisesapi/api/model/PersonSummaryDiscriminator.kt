package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: fullPersonSummary,restrictedPersonSummary,unknownPersonSummary
*/
enum class PersonSummaryDiscriminator(@get:JsonValue val value: kotlin.String) {

    fullPersonSummary("FullPersonSummary"),
    restrictedPersonSummary("RestrictedPersonSummary"),
    unknownPersonSummary("UnknownPersonSummary");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PersonSummaryDiscriminator {
                return values().first{it -> it.value == value}
        }
    }
}

