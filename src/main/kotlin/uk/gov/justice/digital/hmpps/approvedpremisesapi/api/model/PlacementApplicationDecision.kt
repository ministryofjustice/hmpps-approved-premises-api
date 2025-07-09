package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: accepted,rejected,withdraw,withdrawnByPp
*/
enum class PlacementApplicationDecision(@get:JsonValue val value: kotlin.String) {

    accepted("accepted"),
    rejected("rejected"),
    withdraw("withdraw"),
    withdrawnByPp("withdrawn_by_pp");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PlacementApplicationDecision {
                return values().first{it -> it.value == value}
        }
    }
}

