package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param apCode
 * @param postcode
 * @param bedCount
 * @param availableBedsForToday
 * @param bookings
 * @param dateCapacities
 */
data class ExtendedPremisesSummary(

  @Schema(example = "null", description = "")
  @get:JsonProperty("id") val id: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("name") val name: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apCode") val apCode: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("postcode") val postcode: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bedCount") val bedCount: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("availableBedsForToday") val availableBedsForToday: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bookings") val bookings: kotlin.collections.List<PremisesBooking>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dateCapacities") val dateCapacities: kotlin.collections.List<DateCapacity>? = null,
)
