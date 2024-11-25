package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: manual,automatic
*/
enum class RequestForPlacementType(val value: kotlin.String) {

  @JsonProperty("manual")
  manual("manual"),

  @JsonProperty("automatic")
  automatic("automatic"),
}
