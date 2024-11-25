package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param premisesId
 * @param premisesName
 * @param arrivalDate
 * @param departureDate
 * @param createdAt
 * @param type
 */
data class BookingSummary(

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
  @get:JsonProperty("type", required = true) val type: BookingSummary.Type,
) {

  /**
   *
   * Values: space,legacy
   */
  enum class Type(val value: kotlin.String) {

    @JsonProperty("space")
    space("space"),

    @JsonProperty("legacy")
    legacy("legacy"),
  }
}
