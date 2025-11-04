package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TaskSortField(@get:JsonValue val value: kotlin.String) {

  createdAt("createdAt"),
  dueAt("dueAt"),
  person("person"),
  allocatedTo("allocatedTo"),
  completedAt("completedAt"),
  taskType("taskType"),
  decision("decision"),
  expectedArrivalDate("expectedArrivalDate"),
  apType("apType"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TaskSortField = values().first { it -> it.value == value }
  }
}
