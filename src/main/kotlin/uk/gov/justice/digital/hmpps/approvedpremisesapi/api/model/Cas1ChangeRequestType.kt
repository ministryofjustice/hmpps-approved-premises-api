package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ChangeRequestType(@get:JsonValue val value: String) {

  PLACEMENT_APPEAL("placementAppeal"),
  PLACEMENT_EXTENSION("placementExtension"),
  PLANNED_TRANSFER("plannedTransfer"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1ChangeRequestType = values().first { it.value == value }
  }
}
