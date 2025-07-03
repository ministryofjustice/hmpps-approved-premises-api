package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * Values: pdu,la
 */
enum class Cas3PremisesSortBy(@get:JsonValue val value: String) {

    pdu("pdu"),
    la("la");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3PremisesSortBy {
            return values().first { it -> it.value == value }
        }
    }
}

