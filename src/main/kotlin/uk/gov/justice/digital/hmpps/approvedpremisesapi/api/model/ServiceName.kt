package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: approvedPremises,cas2,temporaryAccommodation
*/
enum class ServiceName(val value: kotlin.String) {

  @JsonProperty("approved-premises")
  approvedPremises("approved-premises"),

  @JsonProperty("cas2")
  cas2("cas2"),

  @JsonProperty("temporary-accommodation")
  temporaryAccommodation("temporary-accommodation"),
}
