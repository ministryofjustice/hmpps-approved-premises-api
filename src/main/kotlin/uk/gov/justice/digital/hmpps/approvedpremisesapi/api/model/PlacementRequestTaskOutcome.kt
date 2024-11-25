package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: matched,unableToMatch
*/
enum class PlacementRequestTaskOutcome(val value: kotlin.String) {

  @JsonProperty("matched")
  matched("matched"),

  @JsonProperty("unable_to_match")
  unableToMatch("unable_to_match"),
}
