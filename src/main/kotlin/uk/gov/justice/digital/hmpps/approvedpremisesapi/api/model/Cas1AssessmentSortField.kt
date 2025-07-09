package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: personName,personCrn,assessmentArrivalDate,assessmentStatus,assessmentCreatedAt,assessmentDueAt,applicationProbationDeliveryUnitName
*/
enum class Cas1AssessmentSortField(@get:JsonValue val value: kotlin.String) {

    personName("name"),
    personCrn("crn"),
    assessmentArrivalDate("arrivalDate"),
    assessmentStatus("status"),
    assessmentCreatedAt("createdAt"),
    assessmentDueAt("dueAt"),
    applicationProbationDeliveryUnitName("probationDeliveryUnitName");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1AssessmentSortField {
                return values().first{it -> it.value == value}
        }
    }
}

