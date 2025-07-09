package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: notMatched,unableToMatch,matched
*/
enum class PlacementRequestStatus(@get:JsonValue val value: kotlin.String) {

    notMatched("notMatched"),
    unableToMatch("unableToMatch"),
    matched("matched");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PlacementRequestStatus {
                return values().first{it -> it.value == value}
        }
    }
}

