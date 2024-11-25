package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: notMatched,unableToMatch,matched
*/
enum class PlacementRequestStatus(val value: kotlin.String) {

  @JsonProperty("notMatched")
  notMatched("notMatched"),

  @JsonProperty("unableToMatch")
  unableToMatch("unableToMatch"),

  @JsonProperty("matched")
  matched("matched"),
}
