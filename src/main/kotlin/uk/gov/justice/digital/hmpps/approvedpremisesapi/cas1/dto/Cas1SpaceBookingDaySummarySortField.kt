package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceBookingDaySummarySortField(@get:JsonValue val value: String) {

  PERSON_NAME("personName"),
  PERSON_TIER("personTier"),
  TIER("tier"),
  CANONICAL_ARRIVAL_DATE("canonicalArrivalDate"),
  CANONICAL_DEPARTURE_DATE("canonicalDepartureDate"),
  RELEASE_TYPE("releaseType"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1SpaceBookingDaySummarySortField = entries.first { it.value == value }
  }
}
