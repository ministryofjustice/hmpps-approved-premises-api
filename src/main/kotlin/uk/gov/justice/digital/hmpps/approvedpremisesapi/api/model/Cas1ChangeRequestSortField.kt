package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: NAME,TIER,CANONICAL_ARRIVAL_DATE
*/
enum class Cas1ChangeRequestSortField(@get:JsonValue val value: kotlin.String) {

  NAME("name"),
  TIER("tier"),
  CANONICAL_ARRIVAL_DATE("canonicalArrivalDate"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ChangeRequestSortField = entries.first { it.value == value }
  }
}
