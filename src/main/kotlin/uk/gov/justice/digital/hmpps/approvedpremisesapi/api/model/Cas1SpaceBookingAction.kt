package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: APPEAL_CREATE,PLANNED_TRANSFER_REQUEST,EMERGENCY_TRANSFER_CREATE,SHORTEN
*/
enum class Cas1SpaceBookingAction(@get:JsonValue val value: kotlin.String) {

    APPEAL_CREATE("appealCreate"),
    PLANNED_TRANSFER_REQUEST("plannedTransferRequest"),
    EMERGENCY_TRANSFER_CREATE("emergencyTransferCreate"),
    SHORTEN("shorten");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1SpaceBookingAction {
                return values().first{it -> it.value == value}
        }
    }
}

