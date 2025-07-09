package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* We should use Cas1ApplicationStatus instead, which duplicates this
* Values: started,rejected,awaitingAssesment,unallocatedAssesment,assesmentInProgress,awaitingPlacement,placementAllocated,inapplicable,withdrawn,requestedFurtherInformation,pendingPlacementRequest,expired
*/
enum class ApprovedPremisesApplicationStatus(@get:JsonValue val value: kotlin.String) {

    started("started"),
    rejected("rejected"),
    awaitingAssesment("awaitingAssesment"),
    unallocatedAssesment("unallocatedAssesment"),
    assesmentInProgress("assesmentInProgress"),
    awaitingPlacement("awaitingPlacement"),
    placementAllocated("placementAllocated"),
    inapplicable("inapplicable"),
    withdrawn("withdrawn"),
    requestedFurtherInformation("requestedFurtherInformation"),
    pendingPlacementRequest("pendingPlacementRequest"),
    expired("expired");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApprovedPremisesApplicationStatus {
                return values().first{it -> it.value == value}
        }
    }
}

