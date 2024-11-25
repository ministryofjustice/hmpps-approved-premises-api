package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: createdAt,dueAt,person,allocatedTo,completedAt,taskType,decision
*/
enum class TaskSortField(val value: kotlin.String) {

  @JsonProperty("createdAt")
  createdAt("createdAt"),

  @JsonProperty("dueAt")
  dueAt("dueAt"),

  @JsonProperty("person")
  person("person"),

  @JsonProperty("allocatedTo")
  allocatedTo("allocatedTo"),

  @JsonProperty("completedAt")
  completedAt("completedAt"),

  @JsonProperty("taskType")
  taskType("taskType"),

  @JsonProperty("decision")
  decision("decision"),
}
