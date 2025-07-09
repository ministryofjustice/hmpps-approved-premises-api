package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: awaitingResponse,completed,reallocated,inProgress,notStarted
*/
enum class Cas1AssessmentStatus(@get:JsonValue val value: kotlin.String) {

    awaitingResponse("awaiting_response"),
    completed("completed"),
    reallocated("reallocated"),
    inProgress("in_progress"),
    notStarted("not_started");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1AssessmentStatus {
                return values().first{it -> it.value == value}
        }
    }
}

