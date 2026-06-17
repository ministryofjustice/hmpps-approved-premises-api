package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ApplicationStatus(@get:JsonValue val value: String) {

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
    fun forValue(value: String): Cas1ApplicationStatus = values().first { it.value == value }
  }
}
