package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: standard,emergency,shortNotice
*/
enum class Cas1ApplicationTimelinessCategory(val value: kotlin.String) {

  @JsonProperty("standard")
  standard("standard"),

  @JsonProperty("emergency")
  emergency("emergency"),

  @JsonProperty("shortNotice")
  shortNotice("shortNotice"),
}
