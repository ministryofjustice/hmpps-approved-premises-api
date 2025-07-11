package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: PERSON_NAME,TIER,CANONICAL_ARRIVAL_DATE,CANONICAL_DEPARTURE_DATE,RELEASE_TYPE
*/
enum class Cas1SpaceBookingDaySummarySortField(@get:JsonValue val value: kotlin.String) {

  PERSON_NAME("personName"),
  TIER("tier"),
  CANONICAL_ARRIVAL_DATE("canonicalArrivalDate"),
  CANONICAL_DEPARTURE_DATE("canonicalDepartureDate"),
  RELEASE_TYPE("releaseType"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingDaySummarySortField = entries.first { it.value == value }
  }
}
