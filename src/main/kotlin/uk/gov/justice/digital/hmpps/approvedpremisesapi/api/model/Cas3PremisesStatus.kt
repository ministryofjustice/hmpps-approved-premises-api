package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * Values: online,archived
 */
enum class Cas3PremisesStatus(@get:JsonValue val value: String) {

    online("online"),
    archived("archived");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3PremisesStatus {
            return values().first { it -> it.value == value }
        }
    }
}

