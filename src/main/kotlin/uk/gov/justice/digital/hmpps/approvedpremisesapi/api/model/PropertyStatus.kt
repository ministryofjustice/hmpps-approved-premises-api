package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: pending,active,archived
*/
enum class PropertyStatus(val value: kotlin.String) {

  @JsonProperty("pending")
  pending("pending"),

  @JsonProperty("active")
  active("active"),

  @JsonProperty("archived")
  archived("archived"),
}
