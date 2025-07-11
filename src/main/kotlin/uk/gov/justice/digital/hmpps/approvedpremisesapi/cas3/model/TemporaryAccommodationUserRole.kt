package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: assessor,referrer,reporter
*/
enum class TemporaryAccommodationUserRole(@get:JsonValue val value: String) {

    assessor("assessor"),
    referrer("referrer"),
    reporter("reporter");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): TemporaryAccommodationUserRole {
                return values().first{it -> it.value == value}
        }
    }
}

