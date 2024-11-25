package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: asc,desc
*/
enum class SortDirection(val value: kotlin.String) {

  @JsonProperty("asc")
  asc("asc"),

  @JsonProperty("desc")
  desc("desc"),
}
