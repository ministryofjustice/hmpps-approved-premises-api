package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class TemporaryAccommodationAssessmentStatus(val value: String) {

  unallocated("unallocated"),
  inReview("in_review"),
  readyToPlace("ready_to_place"),
  closed("closed"),
  rejected("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): TemporaryAccommodationAssessmentStatus = entries.first { it.value == value }
  }
}
