package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: APPEAL_CREATE,PLANNED_TRANSFER_REQUEST,EMERGENCY_TRANSFER_CREATE,SHORTEN
*/
enum class Cas1SpaceBookingAction(@get:JsonValue val value: kotlin.String) {

  APPEAL_CREATE("appealCreate"),
  PLANNED_TRANSFER_REQUEST("plannedTransferRequest"),
  EMERGENCY_TRANSFER_CREATE("emergencyTransferCreate"),
  SHORTEN("shorten"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingAction = entries.first { it.value == value }
  }
}
