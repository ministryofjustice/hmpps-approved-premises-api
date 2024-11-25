package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: ascending,descending
*/
enum class SortOrder(val value: kotlin.String) {

  @JsonProperty("ascending")
  ascending("ascending"),

  @JsonProperty("descending")
  descending("descending"),
}
