package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: created,updatedStartDate,updatedEndDate,updatedReferenceNumber,updatedReason,updatedNotes
*/
enum class Cas1OutOfServiceBedRevisionType(@get:JsonValue val value: kotlin.String) {

    created("created"),
    updatedStartDate("updatedStartDate"),
    updatedEndDate("updatedEndDate"),
    updatedReferenceNumber("updatedReferenceNumber"),
    updatedReason("updatedReason"),
    updatedNotes("updatedNotes");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1OutOfServiceBedRevisionType {
                return values().first{it -> it.value == value}
        }
    }
}

