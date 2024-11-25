package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: retrieved,notFound,error
*/
enum class RiskEnvelopeStatus(val value: kotlin.String) {

  @JsonProperty("retrieved")
  retrieved("retrieved"),

  @JsonProperty("not_found")
  notFound("not_found"),

  @JsonProperty("error")
  error("error"),
}
