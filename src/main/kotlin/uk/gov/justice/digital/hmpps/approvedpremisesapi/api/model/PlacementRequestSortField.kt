package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: duration,expectedArrival,createdAt,applicationSubmittedAt,requestType,personName,personRisksTier
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementRequestSortField(@get:JsonValue val value: kotlin.String) {

  duration("duration"),
  expectedArrival("expected_arrival"),
  createdAt("created_at"),
  applicationSubmittedAt("application_date"),
  requestType("request_type"),
  personName("person_name"),
  personRisksTier("person_risks_tier"),
  firstBookingPremisesName("name"),
  firstBookingArrivalDate("canonical_arrival_date"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementRequestSortField = values().first { it -> it.value == value }
  }
}
