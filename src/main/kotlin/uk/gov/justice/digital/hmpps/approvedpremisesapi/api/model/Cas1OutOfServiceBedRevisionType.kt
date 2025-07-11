package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: created,updatedStartDate,updatedEndDate,updatedReferenceNumber,updatedReason,updatedNotes
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1OutOfServiceBedRevisionType(@get:JsonValue val value: kotlin.String) {

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
    fun forValue(value: kotlin.String): Cas1OutOfServiceBedRevisionType = entries.first { it.value == value }
  }
}
