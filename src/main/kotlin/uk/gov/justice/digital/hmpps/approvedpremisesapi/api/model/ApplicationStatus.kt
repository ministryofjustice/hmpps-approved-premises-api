package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationStatus(@get:JsonValue val value: String, val priority: Int) {

  rejected("rejected", 0),
  inProgress("inProgress", 1),
  submitted("submitted", 2),
  requestedFurtherInformation("requestedFurtherInformation", 3),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): ApplicationStatus = entries.first { it -> it.value == value }
  }
}
