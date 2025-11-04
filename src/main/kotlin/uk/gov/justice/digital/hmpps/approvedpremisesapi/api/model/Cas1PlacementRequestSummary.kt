package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class Cas1PlacementRequestSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("placementRequestStatus", required = true) val placementRequestStatus: Cas1PlacementRequestSummary.PlacementRequestStatus,

  @get:JsonProperty("isParole", required = true) val isParole: kotlin.Boolean,

  @get:JsonProperty("requestedPlacementDuration") val requestedPlacementDuration: kotlin.Int? = null,

  @get:JsonProperty("requestedPlacementArrivalDate") val requestedPlacementArrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("personTier") val personTier: kotlin.String? = null,

  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @get:JsonProperty("applicationSubmittedDate") val applicationSubmittedDate: java.time.LocalDate? = null,

  @get:JsonProperty("firstBookingPremisesName") val firstBookingPremisesName: kotlin.String? = null,

  @get:JsonProperty("firstBookingArrivalDate") val firstBookingArrivalDate: java.time.LocalDate? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class PlacementRequestStatus(@get:JsonValue val value: kotlin.String) {

    matched("matched"),
    unableToMatch("unableToMatch"),
    notMatched("notMatched"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): PlacementRequestStatus = values().first { it -> it.value == value }
    }
  }
}
