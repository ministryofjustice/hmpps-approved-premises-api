package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: notStarted,inProgress,complete,infoRequested
*/
enum class TaskStatus(val value: kotlin.String) {

  @JsonProperty("not_started")
  notStarted("not_started"),

  @JsonProperty("in_progress")
  inProgress("in_progress"),

  @JsonProperty("complete")
  complete("complete"),

  @JsonProperty("info_requested")
  infoRequested("info_requested"),
}
