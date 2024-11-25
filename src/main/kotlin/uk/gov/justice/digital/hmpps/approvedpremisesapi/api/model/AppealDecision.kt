package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: accepted,rejected
*/
enum class AppealDecision(val value: kotlin.String) {

  @JsonProperty("accepted")
  accepted("accepted"),

  @JsonProperty("rejected")
  rejected("rejected"),
}
