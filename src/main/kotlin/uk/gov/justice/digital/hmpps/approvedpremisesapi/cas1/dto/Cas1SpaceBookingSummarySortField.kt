package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

object Cas1SpaceBookingSummarySortFieldConstants {
  const val DESCRIPTION: String = "'tier' sorts on the tier captured when the application was created"
}

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
@Schema(description = Cas1SpaceBookingSummarySortFieldConstants.DESCRIPTION)
enum class Cas1SpaceBookingSummarySortField(@get:JsonValue val value: String) {

  personName("personName"),
  canonicalArrivalDate("canonicalArrivalDate"),
  canonicalDepartureDate("canonicalDepartureDate"),
  keyWorkerName("keyWorkerName"),
  tier("tier"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1SpaceBookingSummarySortField = values().first { it.value == value }
  }
}
