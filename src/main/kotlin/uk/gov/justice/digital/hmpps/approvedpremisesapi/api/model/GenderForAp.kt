package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: male,female
*/
enum class GenderForAp(val value: kotlin.String) {

  @JsonProperty("male")
  male("male"),

  @JsonProperty("female")
  female("female"),
}
