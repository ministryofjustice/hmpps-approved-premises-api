package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceBookingAction(@get:JsonValue val value: kotlin.String) {

  APPEAL_CREATE("appealCreate"),
  PLANNED_TRANSFER_REQUEST("plannedTransferRequest"),
  EMERGENCY_TRANSFER_CREATE("emergencyTransferCreate"),
  SHORTEN("shorten"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingAction = values().first { it -> it.value == value }
  }
}
