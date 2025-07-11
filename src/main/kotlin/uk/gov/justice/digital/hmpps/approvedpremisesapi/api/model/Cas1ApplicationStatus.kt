package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: started,rejected,awaitingAssesment,unallocatedAssesment,assesmentInProgress,awaitingPlacement,placementAllocated,inapplicable,withdrawn,requestedFurtherInformation,pendingPlacementRequest,expired
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ApplicationStatus(@get:JsonValue val value: kotlin.String) {

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
    fun forValue(value: kotlin.String): Cas1ApplicationStatus = values().first { it -> it.value == value }
  }
}
