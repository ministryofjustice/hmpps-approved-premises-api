package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: tier,createdAt,arrivalDate,releaseType
*/
enum class ApplicationSortField(val value: kotlin.String) {

  @JsonProperty("tier")
  tier("tier"),

  @JsonProperty("createdAt")
  createdAt("createdAt"),

  @JsonProperty("arrivalDate")
  arrivalDate("arrivalDate"),

  @JsonProperty("releaseType")
  releaseType("releaseType"),
}
