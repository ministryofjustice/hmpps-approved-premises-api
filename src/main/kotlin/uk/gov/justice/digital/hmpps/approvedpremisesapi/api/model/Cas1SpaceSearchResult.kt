package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceSearchResult(

  @get:JsonProperty("premises", required = true) val premises: Cas1PremisesSearchResultSummary,

  @field:Schema(example = "2.1", required = true, description = "")
  @get:JsonProperty("distanceInMiles", required = true) val distanceInMiles: java.math.BigDecimal,
)
