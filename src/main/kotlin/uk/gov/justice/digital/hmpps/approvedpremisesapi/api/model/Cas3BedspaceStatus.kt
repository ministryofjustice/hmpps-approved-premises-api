package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: online,archived,upcoming
*/
enum class Cas3BedspaceStatus(@get:JsonValue val value: String) {

    online("online"),
    archived("archived"),
    upcoming("upcoming");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3BedspaceStatus {
                return values().first{it -> it.value == value}
        }
    }
}

