package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic

/**
 *
 * @param id
 * @param premisesId
 * @param premisesName
 * @param arrivalDate
 * @param departureDate
 * @param createdAt
 * @param type
 * @param characteristics
 */
data class PlacementRequestBookingSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premisesId", required = true) val premisesId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premisesName", required = true) val premisesName: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: PlacementRequestBookingSummary.Type,

  @Schema(example = "null", description = "")
  @get:JsonProperty("characteristics") val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,
) {

  /**
   *
   * Values: space,legacy
   */
  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Type(@get:JsonValue val value: kotlin.String) {

    space("space"),
    legacy("legacy"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): Type = values().first { it -> it.value == value }
    }
  }
}
