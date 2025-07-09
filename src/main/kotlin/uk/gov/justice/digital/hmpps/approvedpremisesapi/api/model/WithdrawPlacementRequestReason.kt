package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: duplicatePlacementRequest,alternativeProvisionIdentified,changeInCircumstances,changeInReleaseDecision,noCapacityDueToLostBed,noCapacityDueToPlacementPrioritisation,noCapacity,errorInPlacementRequest,withdrawnByPP,relatedApplicationWithdrawn,relatedPlacementRequestWithdrawn,relatedPlacementApplicationWithdrawn
*/
enum class WithdrawPlacementRequestReason(@get:JsonValue val value: kotlin.String) {

    duplicatePlacementRequest("DuplicatePlacementRequest"),
    alternativeProvisionIdentified("AlternativeProvisionIdentified"),
    changeInCircumstances("ChangeInCircumstances"),
    changeInReleaseDecision("ChangeInReleaseDecision"),
    noCapacityDueToLostBed("NoCapacityDueToLostBed"),
    noCapacityDueToPlacementPrioritisation("NoCapacityDueToPlacementPrioritisation"),
    noCapacity("NoCapacity"),
    errorInPlacementRequest("ErrorInPlacementRequest"),
    withdrawnByPP("WithdrawnByPP"),
    relatedApplicationWithdrawn("RelatedApplicationWithdrawn"),
    relatedPlacementRequestWithdrawn("RelatedPlacementRequestWithdrawn"),
    relatedPlacementApplicationWithdrawn("RelatedPlacementApplicationWithdrawn");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): WithdrawPlacementRequestReason {
                return values().first{it -> it.value == value}
        }
    }
}

