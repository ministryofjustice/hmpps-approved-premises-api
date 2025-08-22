package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class WithdrawableType(@get:JsonValue val value: String) {

  application("application"),
  booking("booking"),
  placementApplication("placement_application"),
  spaceBooking("space_booking"),
}
