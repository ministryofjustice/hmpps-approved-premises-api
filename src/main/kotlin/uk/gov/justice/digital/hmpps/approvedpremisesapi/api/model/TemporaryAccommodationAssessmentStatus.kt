package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: unallocated,inReview,readyToPlace,closed,rejected
*/
enum class TemporaryAccommodationAssessmentStatus(val value: kotlin.String) {

  @JsonProperty("unallocated")
  unallocated("unallocated"),

  @JsonProperty("in_review")
  inReview("in_review"),

  @JsonProperty("ready_to_place")
  readyToPlace("ready_to_place"),

  @JsonProperty("closed")
  closed("closed"),

  @JsonProperty("rejected")
  rejected("rejected"),
}
