package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: awaitingResponse,completed,reallocated,inProgress,notStarted
*/
enum class ApprovedPremisesAssessmentStatus(val value: kotlin.String) {

  @JsonProperty("awaiting_response")
  awaitingResponse("awaiting_response"),

  @JsonProperty("completed")
  completed("completed"),

  @JsonProperty("reallocated")
  reallocated("reallocated"),

  @JsonProperty("in_progress")
  inProgress("in_progress"),

  @JsonProperty("not_started")
  notStarted("not_started"),
}
