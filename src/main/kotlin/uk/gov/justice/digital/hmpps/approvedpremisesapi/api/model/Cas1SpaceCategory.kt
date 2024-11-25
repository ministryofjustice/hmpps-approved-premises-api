package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
* Spaces are categorised by these traits - 'standard' plus selected physcial and risk characteristics
* Values: standard,arson,wheelchair,sexOffender,enSuite,single
*/
enum class Cas1SpaceCategory(val value: kotlin.String) {

  @JsonProperty("standard")
  standard("standard"),

  @JsonProperty("arson")
  arson("arson"),

  @JsonProperty("wheelchair")
  wheelchair("wheelchair"),

  @JsonProperty("sexOffender")
  sexOffender("sexOffender"),

  @JsonProperty("enSuite")
  enSuite("enSuite"),

  @JsonProperty("single")
  single("single"),
}
