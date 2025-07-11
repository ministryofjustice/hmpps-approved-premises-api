package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: personName,canonicalArrivalDate,canonicalDepartureDate,keyWorkerName,tier
*/
enum class Cas1SpaceBookingSummarySortField(@get:JsonValue val value: kotlin.String) {

  personName("personName"),
  canonicalArrivalDate("canonicalArrivalDate"),
  canonicalDepartureDate("canonicalDepartureDate"),
  keyWorkerName("keyWorkerName"),
  tier("tier"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingSummarySortField = values().first { it -> it.value == value }
  }
}
