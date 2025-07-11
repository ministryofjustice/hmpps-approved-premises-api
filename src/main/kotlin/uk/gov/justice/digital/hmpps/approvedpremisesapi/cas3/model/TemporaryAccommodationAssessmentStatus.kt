package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: unallocated,inReview,readyToPlace,closed,rejected
*/
enum class TemporaryAccommodationAssessmentStatus(@get:JsonValue val value: String) {

    unallocated("unallocated"),
    inReview("in_review"),
    readyToPlace("ready_to_place"),
    closed("closed"),
    rejected("rejected");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): TemporaryAccommodationAssessmentStatus {
                return values().first{it -> it.value == value}
        }
    }
}

