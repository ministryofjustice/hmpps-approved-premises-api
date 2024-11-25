package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param apCode
 * @param postcode
 * @param apArea
 * @param bedCount The total number of spaces in this premises
 * @param availableBeds The total number of spaces available right now
 * @param outOfServiceBeds The total number of out of service beds
 * @param supportsSpaceBookings
 */
data class Cas1PremisesSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "NEHOPE1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

  @Schema(example = "LS1 3AD", required = true, description = "")
  @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: ApArea,

  @Schema(example = "22", required = true, description = "The total number of spaces in this premises")
  @get:JsonProperty("bedCount", required = true) val bedCount: kotlin.Int,

  @Schema(example = "20", required = true, description = "The total number of spaces available right now")
  @get:JsonProperty("availableBeds", required = true) val availableBeds: kotlin.Int,

  @Schema(example = "2", required = true, description = "The total number of out of service beds")
  @get:JsonProperty("outOfServiceBeds", required = true) val outOfServiceBeds: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("supportsSpaceBookings", required = true) val supportsSpaceBookings: kotlin.Boolean,
)
