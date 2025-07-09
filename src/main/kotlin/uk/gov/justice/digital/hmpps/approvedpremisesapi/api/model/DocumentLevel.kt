package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* The level at which a Document is associated - i.e. to the Offender or to a specific Conviction
* Values: offender,conviction
*/
enum class DocumentLevel(@get:JsonValue val value: kotlin.String) {

    offender("Offender"),
    conviction("Conviction");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): DocumentLevel {
                return values().first{it -> it.value == value}
        }
    }
}

