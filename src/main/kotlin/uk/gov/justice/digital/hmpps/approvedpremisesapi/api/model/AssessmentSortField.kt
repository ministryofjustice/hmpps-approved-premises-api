package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: personName,personCrn,assessmentArrivalDate,assessmentStatus,assessmentCreatedAt,assessmentDueAt,applicationProbationDeliveryUnitName
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class AssessmentSortField(@get:JsonValue val value: kotlin.String) {

  personName("name"),
  personCrn("crn"),
  assessmentArrivalDate("arrivalDate"),
  assessmentStatus("status"),
  assessmentCreatedAt("createdAt"),
  assessmentDueAt("dueAt"),
  applicationProbationDeliveryUnitName("probationDeliveryUnitName"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): AssessmentSortField = entries.first { it.value == value }
  }
}
