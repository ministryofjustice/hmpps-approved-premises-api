package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: fullPersonSummary,restrictedPersonSummary,unknownPersonSummary
*/
enum class PersonSummaryDiscriminator(val value: kotlin.String) {

  @JsonProperty("FullPersonSummary")
  fullPersonSummary("FullPersonSummary"),

  @JsonProperty("RestrictedPersonSummary")
  restrictedPersonSummary("RestrictedPersonSummary"),

  @JsonProperty("UnknownPersonSummary")
  unknownPersonSummary("UnknownPersonSummary"),
}
