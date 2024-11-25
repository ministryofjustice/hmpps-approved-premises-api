package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: parole,standardRelease
*/
enum class PlacementRequestRequestType(val value: kotlin.String) {

  @JsonProperty("parole")
  parole("parole"),

  @JsonProperty("standardRelease")
  standardRelease("standardRelease"),
}
