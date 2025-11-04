package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Cas1PlacementRequestSummary(

  val id: java.util.UUID,

  val person: Person,

  val placementRequestStatus: Cas1PlacementRequestSummary.PlacementRequestStatus,

  val isParole: kotlin.Boolean,

  val requestedPlacementDuration: kotlin.Int? = null,

  val requestedPlacementArrivalDate: java.time.LocalDate? = null,

  val personTier: kotlin.String? = null,

  val applicationId: java.util.UUID? = null,

  val applicationSubmittedDate: java.time.LocalDate? = null,

  val firstBookingPremisesName: kotlin.String? = null,

  val firstBookingArrivalDate: java.time.LocalDate? = null,
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
