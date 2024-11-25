package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: premisesName,roomName,bedName,startDate,endDate,reason,daysLost
*/
enum class Cas1OutOfServiceBedSortField(val value: kotlin.String) {

  @JsonProperty("premisesName")
  premisesName("premisesName"),

  @JsonProperty("roomName")
  roomName("roomName"),

  @JsonProperty("bedName")
  bedName("bedName"),

  @JsonProperty("startDate")
  startDate("startDate"),

  @JsonProperty("endDate")
  endDate("endDate"),

  @JsonProperty("reason")
  reason("reason"),

  @JsonProperty("daysLost")
  daysLost("daysLost"),
}
