package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
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
  relatedPlacementApplicationWithdrawn("RelatedPlacementApplicationWithdrawn"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): WithdrawPlacementRequestReason = values().first { it -> it.value == value }
  }
}
