package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: ALLOCATED,DEALLOCATED,IN_PROGRESS,PRISON,UNALLOCATED
*/
enum class AssignmentType(@get:JsonValue val value: String) {

    ALLOCATED("ALLOCATED"),
    DEALLOCATED("DEALLOCATED"),
    IN_PROGRESS("IN_PROGRESS"),
    PRISON("PRISON"),
    UNALLOCATED("UNALLOCATED");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): AssignmentType {
                return values().first{it -> it.value == value}
        }
    }
}

