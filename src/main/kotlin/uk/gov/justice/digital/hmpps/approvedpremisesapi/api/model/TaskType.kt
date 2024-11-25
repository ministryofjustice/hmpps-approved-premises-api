package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: assessment,placementRequest,placementApplication,bookingAppeal
*/
enum class TaskType(val value: kotlin.String) {

  @JsonProperty("Assessment")
  assessment("Assessment"),

  @JsonProperty("PlacementRequest")
  placementRequest("PlacementRequest"),

  @JsonProperty("PlacementApplication")
  placementApplication("PlacementApplication"),

  @JsonProperty("BookingAppeal")
  bookingAppeal("BookingAppeal"),
}
