package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: user,system
*/
enum class TriggerSourceType(val value: kotlin.String) {

  @JsonProperty("user")
  user("user"),

  @JsonProperty("system")
  system("system"),
}
