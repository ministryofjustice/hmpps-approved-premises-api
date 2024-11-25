package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: sharedProperty,singleOccupancy,wheelchairAccessible
*/
enum class BedSearchAttributes(val value: kotlin.String) {

  @JsonProperty("sharedProperty")
  sharedProperty("sharedProperty"),

  @JsonProperty("singleOccupancy")
  singleOccupancy("singleOccupancy"),

  @JsonProperty("wheelchairAccessible")
  wheelchairAccessible("wheelchairAccessible"),
}
