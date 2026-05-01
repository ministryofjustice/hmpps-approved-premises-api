package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class PlacementRequirements(

  val type: ApType,

  @Schema(example = "B74", required = true, description = "Postcode outcode")
  val location: kotlin.String,

  val radius: kotlin.Int,

  val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

  val desirableCriteria: kotlin.collections.List<PlacementCriteria>,
)
