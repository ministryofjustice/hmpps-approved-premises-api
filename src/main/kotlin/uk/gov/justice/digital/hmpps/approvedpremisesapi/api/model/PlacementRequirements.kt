package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementRequirements(

  @get:JsonProperty("type", required = true) val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  @get:JsonProperty("location", required = true) val location: String,

  @get:JsonProperty("radius", required = true) val radius: Int,

  @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: List<PlacementCriteria>,

  @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: List<PlacementCriteria>,
)
