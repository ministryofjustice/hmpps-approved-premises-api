package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria

/**
 *
 * @param type
 * @param location Postcode outcode
 * @param radius
 * @param essentialCriteria
 * @param desirableCriteria
 */
data class PlacementRequirements(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  @get:JsonProperty("location", required = true) val location: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("radius", required = true) val radius: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: kotlin.collections.List<PlacementCriteria>,
)
