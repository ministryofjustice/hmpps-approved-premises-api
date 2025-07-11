package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: application,booking,placementApplication,placementRequest,spaceBooking
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class WithdrawableType(@get:JsonValue val value: kotlin.String) {

  application("application"),
  booking("booking"),
  placementApplication("placement_application"),
  placementRequest("placement_request"),
  spaceBooking("space_booking"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): WithdrawableType = values().first { it -> it.value == value }
  }
}
