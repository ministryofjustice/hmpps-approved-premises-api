package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: accepted,rejected,withdraw,withdrawnByPp
*/
enum class PlacementApplicationDecision(val value: kotlin.String) {

  @JsonProperty("accepted")
  accepted("accepted"),

  @JsonProperty("rejected")
  rejected("rejected"),

  @JsonProperty("withdraw")
  withdraw("withdraw"),

  @JsonProperty("withdrawn_by_pp")
  withdrawnByPp("withdrawn_by_pp"),
}
