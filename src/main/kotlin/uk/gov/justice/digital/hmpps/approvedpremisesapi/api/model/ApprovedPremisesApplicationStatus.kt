package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: started,submitted,rejected,awaitingAssesment,unallocatedAssesment,assesmentInProgress,awaitingPlacement,placementAllocated,inapplicable,withdrawn,requestedFurtherInformation,pendingPlacementRequest,expired
*/
enum class ApprovedPremisesApplicationStatus(val value: kotlin.String) {

  @JsonProperty("started")
  started("started"),

  @JsonProperty("submitted")
  submitted("submitted"),

  @JsonProperty("rejected")
  rejected("rejected"),

  @JsonProperty("awaitingAssesment")
  awaitingAssesment("awaitingAssesment"),

  @JsonProperty("unallocatedAssesment")
  unallocatedAssesment("unallocatedAssesment"),

  @JsonProperty("assesmentInProgress")
  assesmentInProgress("assesmentInProgress"),

  @JsonProperty("awaitingPlacement")
  awaitingPlacement("awaitingPlacement"),

  @JsonProperty("placementAllocated")
  placementAllocated("placementAllocated"),

  @JsonProperty("inapplicable")
  inapplicable("inapplicable"),

  @JsonProperty("withdrawn")
  withdrawn("withdrawn"),

  @JsonProperty("requestedFurtherInformation")
  requestedFurtherInformation("requestedFurtherInformation"),

  @JsonProperty("pendingPlacementRequest")
  pendingPlacementRequest("pendingPlacementRequest"),

  @JsonProperty("expired")
  expired("expired"),
}
