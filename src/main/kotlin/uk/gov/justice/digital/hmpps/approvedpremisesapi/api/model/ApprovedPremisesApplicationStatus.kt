package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* We should use Cas1ApplicationStatus instead, which duplicates this
* Values: started,rejected,awaitingAssesment,unallocatedAssesment,assesmentInProgress,awaitingPlacement,placementAllocated,inapplicable,withdrawn,requestedFurtherInformation,pendingPlacementRequest,expired
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
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
  expired("expired"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApprovedPremisesApplicationStatus = values().first { it -> it.value == value }
  }
}
