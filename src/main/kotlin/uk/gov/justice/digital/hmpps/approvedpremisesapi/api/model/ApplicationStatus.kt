package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: inProgress,submitted,requestedFurtherInformation,pending,rejected,awaitingPlacement,placed,inapplicable,withdrawn
*/
enum class ApplicationStatus(@get:JsonValue val value: kotlin.String) {

    inProgress("inProgress"),
    submitted("submitted"),
    requestedFurtherInformation("requestedFurtherInformation"),
    pending("pending"),
    rejected("rejected"),
    awaitingPlacement("awaitingPlacement"),
    placed("placed"),
    inapplicable("inapplicable"),
    withdrawn("withdrawn");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApplicationStatus {
                return values().first{it -> it.value == value}
        }
    }
}

