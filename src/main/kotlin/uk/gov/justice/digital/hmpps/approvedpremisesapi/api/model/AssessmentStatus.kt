package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: cas1AwaitingResponse,cas1Completed,cas1Reallocated,cas1InProgress,cas1NotStarted,cas3Unallocated,cas3InReview,cas3ReadyToPlace,cas3Closed,cas3Rejected
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class AssessmentStatus(@get:JsonValue val value: kotlin.String) {

  cas1AwaitingResponse("awaiting_response"),
  cas1Completed("completed"),
  cas1Reallocated("reallocated"),
  cas1InProgress("in_progress"),
  cas1NotStarted("not_started"),
  cas3Unallocated("unallocated"),
  cas3InReview("in_review"),
  cas3ReadyToPlace("ready_to_place"),
  cas3Closed("closed"),
  cas3Rejected("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): AssessmentStatus = entries.first { it.value == value }
  }
}
