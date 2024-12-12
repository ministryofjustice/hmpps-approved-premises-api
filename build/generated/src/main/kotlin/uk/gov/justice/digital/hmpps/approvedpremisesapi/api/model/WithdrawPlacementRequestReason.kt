package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: duplicatePlacementRequest,alternativeProvisionIdentified,changeInCircumstances,changeInReleaseDecision,noCapacityDueToLostBed,noCapacityDueToPlacementPrioritisation,noCapacity,errorInPlacementRequest,withdrawnByPP,relatedApplicationWithdrawn,relatedPlacementRequestWithdrawn,relatedPlacementApplicationWithdrawn
*/
enum class WithdrawPlacementRequestReason(val value: kotlin.String) {

    @JsonProperty("DuplicatePlacementRequest") duplicatePlacementRequest("DuplicatePlacementRequest"),
    @JsonProperty("AlternativeProvisionIdentified") alternativeProvisionIdentified("AlternativeProvisionIdentified"),
    @JsonProperty("ChangeInCircumstances") changeInCircumstances("ChangeInCircumstances"),
    @JsonProperty("ChangeInReleaseDecision") changeInReleaseDecision("ChangeInReleaseDecision"),
    @JsonProperty("NoCapacityDueToLostBed") noCapacityDueToLostBed("NoCapacityDueToLostBed"),
    @JsonProperty("NoCapacityDueToPlacementPrioritisation") noCapacityDueToPlacementPrioritisation("NoCapacityDueToPlacementPrioritisation"),
    @JsonProperty("NoCapacity") noCapacity("NoCapacity"),
    @JsonProperty("ErrorInPlacementRequest") errorInPlacementRequest("ErrorInPlacementRequest"),
    @JsonProperty("WithdrawnByPP") withdrawnByPP("WithdrawnByPP"),
    @JsonProperty("RelatedApplicationWithdrawn") relatedApplicationWithdrawn("RelatedApplicationWithdrawn"),
    @JsonProperty("RelatedPlacementRequestWithdrawn") relatedPlacementRequestWithdrawn("RelatedPlacementRequestWithdrawn"),
    @JsonProperty("RelatedPlacementApplicationWithdrawn") relatedPlacementApplicationWithdrawn("RelatedPlacementApplicationWithdrawn")
}

