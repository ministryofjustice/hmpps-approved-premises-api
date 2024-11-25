package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: arrived,awaitingMinusArrival,notMinusArrived,departed,cancelled,provisional,confirmed,closed
*/
enum class BookingStatus(val value: kotlin.String) {

  @JsonProperty("arrived")
  arrived("arrived"),

  @JsonProperty("awaiting-arrival")
  awaitingMinusArrival("awaiting-arrival"),

  @JsonProperty("not-arrived")
  notMinusArrived("not-arrived"),

  @JsonProperty("departed")
  departed("departed"),

  @JsonProperty("cancelled")
  cancelled("cancelled"),

  @JsonProperty("provisional")
  provisional("provisional"),

  @JsonProperty("confirmed")
  confirmed("confirmed"),

  @JsonProperty("closed")
  closed("closed"),
}
