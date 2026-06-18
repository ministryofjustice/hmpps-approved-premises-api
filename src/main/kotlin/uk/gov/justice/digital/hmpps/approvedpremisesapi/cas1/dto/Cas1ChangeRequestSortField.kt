package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ChangeRequestSortField(@get:JsonValue val value: String) {

  NAME("name"),
  TIER("tier"),
  CANONICAL_ARRIVAL_DATE("canonicalArrivalDate"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1ChangeRequestSortField = values().first { it.value == value }
  }
}
