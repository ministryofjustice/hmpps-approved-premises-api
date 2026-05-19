package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TaskStatus(@get:JsonValue val value: String) {

  notStarted("not_started"),
  inProgress("in_progress"),
  complete("complete"),
  infoRequested("info_requested"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TaskStatus = values().first { it.value == value }
  }
}
