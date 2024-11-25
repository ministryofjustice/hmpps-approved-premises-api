package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: d0,d1,d2,d3,c0,c1,c2,c3,b0,b1,b2,b3,a0,a1,a2,a3
*/
enum class RiskTierLevel(val value: kotlin.String) {

  @JsonProperty("D0")
  d0("D0"),

  @JsonProperty("D1")
  d1("D1"),

  @JsonProperty("D2")
  d2("D2"),

  @JsonProperty("D3")
  d3("D3"),

  @JsonProperty("C0")
  c0("C0"),

  @JsonProperty("C1")
  c1("C1"),

  @JsonProperty("C2")
  c2("C2"),

  @JsonProperty("C3")
  c3("C3"),

  @JsonProperty("B0")
  b0("B0"),

  @JsonProperty("B1")
  b1("B1"),

  @JsonProperty("B2")
  b2("B2"),

  @JsonProperty("B3")
  b3("B3"),

  @JsonProperty("A0")
  a0("A0"),

  @JsonProperty("A1")
  a1("A1"),

  @JsonProperty("A2")
  a2("A2"),

  @JsonProperty("A3")
  a3("A3"),
}
