package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1OutOfServiceBedRevisionType(@get:JsonValue val value: String) {

  created("created"),
  updatedStartDate("updatedStartDate"),
  updatedEndDate("updatedEndDate"),
  updatedReferenceNumber("updatedReferenceNumber"),
  updatedReason("updatedReason"),
  updatedNotes("updatedNotes"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1OutOfServiceBedRevisionType = values().first { it -> it.value == value }
  }
}
