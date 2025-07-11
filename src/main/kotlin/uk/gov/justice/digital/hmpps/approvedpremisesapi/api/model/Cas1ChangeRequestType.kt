package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: PLACEMENT_APPEAL,PLACEMENT_EXTENSION,PLANNED_TRANSFER
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ChangeRequestType(@get:JsonValue val value: kotlin.String) {

  PLACEMENT_APPEAL("placementAppeal"),
  PLACEMENT_EXTENSION("placementExtension"),
  PLANNED_TRANSFER("plannedTransfer"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ChangeRequestType = entries.first { it.value == value }
  }
}
