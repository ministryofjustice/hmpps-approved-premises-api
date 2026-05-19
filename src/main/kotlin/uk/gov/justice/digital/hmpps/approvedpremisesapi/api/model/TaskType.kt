package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TaskType(@get:JsonValue val value: String) {

  assessment("Assessment"),
  placementApplication("PlacementApplication"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TaskType = values().first { it.value == value }
  }
}
