package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param person
 * @param placementRequestStatus
 * @param isParole
 * @param requestedPlacementDuration
 * @param requestedPlacementArrivalDate
 * @param personTier
 * @param applicationId
 * @param applicationSubmittedDate
 * @param firstBookingPremisesName
 * @param firstBookingArrivalDate
 */
data class Cas1PlacementRequestSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("placementRequestStatus", required = true) val placementRequestStatus: Cas1PlacementRequestSummary.PlacementRequestStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isParole", required = true) val isParole: kotlin.Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("requestedPlacementDuration") val requestedPlacementDuration: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("requestedPlacementArrivalDate") val requestedPlacementArrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("personTier") val personTier: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationSubmittedDate") val applicationSubmittedDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("firstBookingPremisesName") val firstBookingPremisesName: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("firstBookingArrivalDate") val firstBookingArrivalDate: java.time.LocalDate? = null,
) {

  /**
   *
   * Values: matched,unableToMatch,notMatched
   */
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
