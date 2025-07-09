package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: createdAt,dueAt,person,allocatedTo,completedAt,taskType,decision,expectedArrivalDate,apType
*/
enum class TaskSortField(@get:JsonValue val value: kotlin.String) {

    createdAt("createdAt"),
    dueAt("dueAt"),
    person("person"),
    allocatedTo("allocatedTo"),
    completedAt("completedAt"),
    taskType("taskType"),
    decision("decision"),
    expectedArrivalDate("expectedArrivalDate"),
    apType("apType");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskSortField {
                return values().first{it -> it.value == value}
        }
    }
}

