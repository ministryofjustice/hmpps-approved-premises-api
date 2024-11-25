package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: past,current,future
*/
enum class Temporality(val value: kotlin.String) {

  @JsonProperty("past")
  past("past"),

  @JsonProperty("current")
  current("current"),

  @JsonProperty("future")
  future("future"),
}
