package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: cas1AwaitingResponse,cas1Completed,cas1Reallocated,cas1InProgress,cas1NotStarted,cas3Unallocated,cas3InReview,cas3ReadyToPlace,cas3Closed,cas3Rejected
*/
enum class AssessmentStatus(val value: kotlin.String) {

  @JsonProperty("awaiting_response")
  cas1AwaitingResponse("awaiting_response"),

  @JsonProperty("completed")
  cas1Completed("completed"),

  @JsonProperty("reallocated")
  cas1Reallocated("reallocated"),

  @JsonProperty("in_progress")
  cas1InProgress("in_progress"),

  @JsonProperty("not_started")
  cas1NotStarted("not_started"),

  @JsonProperty("unallocated")
  cas3Unallocated("unallocated"),

  @JsonProperty("in_review")
  cas3InReview("in_review"),

  @JsonProperty("ready_to_place")
  cas3ReadyToPlace("ready_to_place"),

  @JsonProperty("closed")
  cas3Closed("closed"),

  @JsonProperty("rejected")
  cas3Rejected("rejected"),
}
