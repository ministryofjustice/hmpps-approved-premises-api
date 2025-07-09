package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: rotl,releaseFollowingDecision,additionalPlacement
*/
enum class PlacementType(@get:JsonValue val value: kotlin.String) {

    rotl("rotl"),
    releaseFollowingDecision("release_following_decision"),
    additionalPlacement("additional_placement");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PlacementType {
                return values().first{it -> it.value == value}
        }
    }
}

