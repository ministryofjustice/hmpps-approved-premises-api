package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: unallocated,inReview,readyToPlace,closed,rejected
*/
enum class TemporaryAccommodationAssessmentStatus(@get:JsonValue val value: kotlin.String) {

    unallocated("unallocated"),
    inReview("in_review"),
    readyToPlace("ready_to_place"),
    closed("closed"),
    rejected("rejected");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TemporaryAccommodationAssessmentStatus {
                return values().first{it -> it.value == value}
        }
    }
}

