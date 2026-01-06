package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationStatus(@get:JsonValue val value: String) {

  inProgress("inProgress"),
  submitted("submitted"),
  requestedFurtherInformation("requestedFurtherInformation"),
  rejected("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): ApplicationStatus = entries.first { it -> it.value == value }
  }
}
