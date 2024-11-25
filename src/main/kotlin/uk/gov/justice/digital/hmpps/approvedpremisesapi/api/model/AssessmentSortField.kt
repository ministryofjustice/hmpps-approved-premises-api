package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: personName,personCrn,assessmentArrivalDate,assessmentStatus,assessmentCreatedAt,assessmentDueAt,applicationProbationDeliveryUnitName
*/
enum class AssessmentSortField(val value: kotlin.String) {

  @JsonProperty("name")
  personName("name"),

  @JsonProperty("crn")
  personCrn("crn"),

  @JsonProperty("arrivalDate")
  assessmentArrivalDate("arrivalDate"),

  @JsonProperty("status")
  assessmentStatus("status"),

  @JsonProperty("createdAt")
  assessmentCreatedAt("createdAt"),

  @JsonProperty("dueAt")
  assessmentDueAt("dueAt"),

  @JsonProperty("probationDeliveryUnitName")
  applicationProbationDeliveryUnitName("probationDeliveryUnitName"),
}
