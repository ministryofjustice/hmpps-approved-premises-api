package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: awaitingResponse,completed,reallocated,inProgress,notStarted
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApprovedPremisesAssessmentStatus(@get:JsonValue val value: kotlin.String) {

  awaitingResponse("awaiting_response"),
  completed("completed"),
  reallocated("reallocated"),
  inProgress("in_progress"),
  notStarted("not_started"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApprovedPremisesAssessmentStatus = entries.first { it.value == value }
  }
}
