package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class AssignmentType(@get:JsonValue val value: String) {

  ALLOCATED("ALLOCATED"),
  DEALLOCATED("DEALLOCATED"),
  IN_PROGRESS("IN_PROGRESS"),
  PRISON("PRISON"),
  UNALLOCATED("UNALLOCATED"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): AssignmentType = values().first { it -> it.value == value }
  }
}
