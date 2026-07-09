package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

object PlacementRequestSortFieldConstants {
  const val DESCRIPTION: String = "'person_tier' sorts on the person's live tier. 'person_risks_tier' sorts on the tier captured when the application was created"
}

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
@Schema(description = PlacementRequestSortFieldConstants.DESCRIPTION)
enum class PlacementRequestSortField(@get:JsonValue val value: String) {

  duration("duration"),
  expectedArrival("expected_arrival"),
  createdAt("created_at"),
  applicationSubmittedAt("application_date"),
  requestType("request_type"),
  personName("person_name"),

  /**
   * sorts on the tier captured when the application was created
   */
  personRisksTier("person_risks_tier"),

  /**
   * sorts on the person's live tier
   */
  personTier("person_tier"),
  firstBookingPremisesName("name"),
  firstBookingArrivalDate("canonical_arrival_date"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): PlacementRequestSortField = entries.first { it.value == value }
  }
}
