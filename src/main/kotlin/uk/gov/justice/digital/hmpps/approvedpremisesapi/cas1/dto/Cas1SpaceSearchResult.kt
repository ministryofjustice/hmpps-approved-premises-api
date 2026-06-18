package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class Cas1SpaceSearchResult(

  @get:JsonProperty("premises", required = true) val premises: Cas1PremisesSearchResultSummary,

  @Schema(example = "2.1", required = true, description = "")
  @get:JsonProperty("distanceInMiles", required = true) val distanceInMiles: BigDecimal,
)
