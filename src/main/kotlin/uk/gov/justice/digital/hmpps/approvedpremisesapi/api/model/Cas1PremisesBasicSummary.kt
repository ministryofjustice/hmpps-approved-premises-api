package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param apArea
 * @param bedCount
 * @param supportsSpaceBookings
 * @param apCode
 */
data class Cas1PremisesBasicSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @Schema(example = "22", required = true, description = "")
  @get:JsonProperty("bedCount", required = true) val bedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("supportsSpaceBookings", required = true) val supportsSpaceBookings: kotlin.Boolean,

  @Schema(example = "NEHOPE1", description = "")
  @get:JsonProperty("apCode") val apCode: kotlin.String? = null,
)
