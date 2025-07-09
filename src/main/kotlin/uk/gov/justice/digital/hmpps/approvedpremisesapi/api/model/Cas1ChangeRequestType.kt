package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: PLACEMENT_APPEAL,PLACEMENT_EXTENSION,PLANNED_TRANSFER
*/
enum class Cas1ChangeRequestType(@get:JsonValue val value: kotlin.String) {

    PLACEMENT_APPEAL("placementAppeal"),
    PLACEMENT_EXTENSION("placementExtension"),
    PLANNED_TRANSFER("plannedTransfer");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1ChangeRequestType {
                return values().first{it -> it.value == value}
        }
    }
}

