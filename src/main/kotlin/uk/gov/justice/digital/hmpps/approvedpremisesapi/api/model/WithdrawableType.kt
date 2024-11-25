package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: application,booking,placementApplication,placementRequest,spaceBooking
*/
enum class WithdrawableType(val value: kotlin.String) {

  @JsonProperty("application")
  application("application"),

  @JsonProperty("booking")
  booking("booking"),

  @JsonProperty("placement_application")
  placementApplication("placement_application"),

  @JsonProperty("placement_request")
  placementRequest("placement_request"),

  @JsonProperty("space_booking")
  spaceBooking("space_booking"),
}
