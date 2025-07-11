package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: inProgress,submitted,requestedFurtherInformation,pending,rejected,awaitingPlacement,placed,inapplicable,withdrawn
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApplicationStatus(@get:JsonValue val value: kotlin.String) {

  inProgress("inProgress"),
  submitted("submitted"),
  requestedFurtherInformation("requestedFurtherInformation"),
  pending("pending"),
  rejected("rejected"),
  awaitingPlacement("awaitingPlacement"),
  placed("placed"),
  inapplicable("inapplicable"),
  withdrawn("withdrawn"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApplicationStatus = values().first { it -> it.value == value }
  }
}
