package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class RiskTierLevel(@get:JsonValue val value: kotlin.String) {

  d0("D0"),
  d1("D1"),
  d2("D2"),
  d3("D3"),
  c0("C0"),
  c1("C1"),
  c2("C2"),
  c3("C3"),
  b0("B0"),
  b1("B1"),
  b2("B2"),
  b3("B3"),
  a0("A0"),
  a1("A1"),
  a2("A2"),
  a3("A3"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): RiskTierLevel = values().first { it -> it.value == value }
  }
}
