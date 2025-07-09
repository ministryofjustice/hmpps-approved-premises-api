package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: assessment,placementApplication
*/
enum class TaskType(@get:JsonValue val value: kotlin.String) {

    assessment("Assessment"),
    placementApplication("PlacementApplication");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskType {
                return values().first{it -> it.value == value}
        }
    }
}

