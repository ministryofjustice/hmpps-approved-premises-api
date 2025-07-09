package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: personName,personCrn,bookingStartDate,bookingEndDate,bookingCreatedAt
*/
enum class BookingSearchSortField(@get:JsonValue val value: kotlin.String) {

    personName("name"),
    personCrn("crn"),
    bookingStartDate("startDate"),
    bookingEndDate("endDate"),
    bookingCreatedAt("createdAt");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): BookingSearchSortField {
                return values().first{it -> it.value == value}
        }
    }
}

