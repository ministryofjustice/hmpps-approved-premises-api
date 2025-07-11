package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: assessment,placementApplication
*/
enum class TaskType(@get:JsonValue val value: kotlin.String) {

  assessment("Assessment"),
  placementApplication("PlacementApplication"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TaskType = entries.first { it.value == value }
  }
}
