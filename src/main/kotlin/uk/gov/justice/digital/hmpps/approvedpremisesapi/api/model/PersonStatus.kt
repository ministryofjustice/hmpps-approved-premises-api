package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: inCustody,inCommunity,unknown
*/
enum class PersonStatus(val value: kotlin.String) {

  @JsonProperty("InCustody")
  inCustody("InCustody"),

  @JsonProperty("InCommunity")
  inCommunity("InCommunity"),

  @JsonProperty("Unknown")
  unknown("Unknown"),
}
