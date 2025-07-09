package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: EMERGENCY,PLANNED
*/
enum class Cas1TimelineEventTransferType(@get:JsonValue val value: kotlin.String) {

    EMERGENCY("emergency"),
    PLANNED("planned");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1TimelineEventTransferType {
                return values().first{it -> it.value == value}
        }
    }
}

