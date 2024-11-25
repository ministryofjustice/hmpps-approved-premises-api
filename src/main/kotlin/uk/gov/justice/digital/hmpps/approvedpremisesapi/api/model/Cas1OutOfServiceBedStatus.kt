package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: active,cancelled
*/
enum class Cas1OutOfServiceBedStatus(val value: kotlin.String) {

  @JsonProperty("active")
  active("active"),

  @JsonProperty("cancelled")
  cancelled("cancelled"),
}
