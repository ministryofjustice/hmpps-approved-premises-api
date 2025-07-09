package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: duration,expectedArrival,createdAt,applicationSubmittedAt,requestType,personName,personRisksTier
*/
enum class PlacementRequestSortField(@get:JsonValue val value: kotlin.String) {

    duration("duration"),
    expectedArrival("expected_arrival"),
    createdAt("created_at"),
    applicationSubmittedAt("application_date"),
    requestType("request_type"),
    personName("person_name"),
    personRisksTier("person_risks_tier");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PlacementRequestSortField {
                return values().first{it -> it.value == value}
        }
    }
}

