package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: notStarted,inProgress,complete,infoRequested
*/
enum class TaskStatus(@get:JsonValue val value: kotlin.String) {

  notStarted("not_started"),
  inProgress("in_progress"),
  complete("complete"),
  infoRequested("info_requested"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TaskStatus = values().first { it -> it.value == value }
  }
}
