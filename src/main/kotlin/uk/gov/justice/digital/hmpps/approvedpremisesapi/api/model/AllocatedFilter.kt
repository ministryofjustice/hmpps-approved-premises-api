package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: allocated,unallocated
*/
enum class AllocatedFilter(val value: kotlin.String) {

  @JsonProperty("allocated")
  allocated("allocated"),

  @JsonProperty("unallocated")
  unallocated("unallocated"),
}
