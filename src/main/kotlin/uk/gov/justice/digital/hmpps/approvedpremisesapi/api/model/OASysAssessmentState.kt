package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: completed,incomplete
*/
enum class OASysAssessmentState(@get:JsonValue val value: kotlin.String) {

    completed("Completed"),
    incomplete("Incomplete");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): OASysAssessmentState {
                return values().first{it -> it.value == value}
        }
    }
}

