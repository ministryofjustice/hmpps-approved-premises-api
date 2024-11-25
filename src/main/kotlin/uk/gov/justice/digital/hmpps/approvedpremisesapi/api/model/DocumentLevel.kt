package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
* The level at which a Document is associated - i.e. to the Offender or to a specific Conviction
* Values: offender,conviction
*/
enum class DocumentLevel(val value: kotlin.String) {

  @JsonProperty("Offender")
  offender("Offender"),

  @JsonProperty("Conviction")
  conviction("Conviction"),
}
