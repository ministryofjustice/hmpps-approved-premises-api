package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.LocalDate
import java.util.UUID

data class Cas1PlacementRequestSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("placementRequestStatus", required = true) val placementRequestStatus: PlacementRequestStatus,

  @get:JsonProperty("isParole", required = true) val isParole: Boolean,

  @get:JsonProperty("requestedPlacementDuration") val requestedPlacementDuration: Int? = null,

  @get:JsonProperty("requestedPlacementArrivalDate") val requestedPlacementArrivalDate: LocalDate? = null,

  @get:JsonProperty("personTier") val personTier: String? = null,

  @get:JsonProperty("applicationId") val applicationId: UUID? = null,

  @get:JsonProperty("applicationSubmittedDate") val applicationSubmittedDate: LocalDate? = null,

  @get:JsonProperty("firstBookingPremisesName") val firstBookingPremisesName: String? = null,

  @get:JsonProperty("firstBookingArrivalDate") val firstBookingArrivalDate: LocalDate? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class PlacementRequestStatus(@get:JsonValue val value: String) {

    matched("matched"),
    unableToMatch("unableToMatch"),
    notMatched("notMatched"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): PlacementRequestStatus = values().first { it.value == value }
    }
  }
}
