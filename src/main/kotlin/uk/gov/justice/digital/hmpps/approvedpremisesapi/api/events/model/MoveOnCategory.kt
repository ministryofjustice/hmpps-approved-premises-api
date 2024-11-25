package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param description
 * @param legacyMoveOnCategoryCode
 * @param id
 */
data class MoveOnCategory(

  @Schema(example = "B&B / Temp / Short-Term Housing", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: kotlin.String,

  @Schema(example = "MC05", required = true, description = "")
  @get:JsonProperty("legacyMoveOnCategoryCode", required = true) val legacyMoveOnCategoryCode: kotlin.String,

  @Schema(example = "a3c3d3df-1e27-4ee5-aef6-8a0f0471075f", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,
)
