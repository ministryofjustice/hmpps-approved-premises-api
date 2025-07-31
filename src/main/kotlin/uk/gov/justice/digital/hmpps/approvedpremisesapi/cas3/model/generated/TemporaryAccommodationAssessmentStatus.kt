package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: unallocated,inReview,readyToPlace,closed,rejected
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TemporaryAccommodationAssessmentStatus(@get:JsonValue val value: kotlin.String) {

  unallocated("unallocated"),
  inReview("in_review"),
  readyToPlace("ready_to_place"),
  closed("closed"),
  rejected("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): TemporaryAccommodationAssessmentStatus = values().first { it.value == value }
  }
}
