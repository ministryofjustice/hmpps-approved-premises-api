package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceSearchResult(

  val premises: Cas1PremisesSearchResultSummary,

  @Schema(example = "2.1", required = true, description = "")
  val distanceInMiles: java.math.BigDecimal,
)
