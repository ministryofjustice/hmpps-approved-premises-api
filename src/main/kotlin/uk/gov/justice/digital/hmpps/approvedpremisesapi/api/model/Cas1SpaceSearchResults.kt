package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceSearchResults(

  @field:Schema(example = "4", required = true, description = "")
  @get:JsonProperty("resultsCount", required = true) val resultsCount: kotlin.Int,

  @get:JsonProperty("results", required = true) val results: kotlin.collections.List<Cas1SpaceSearchResult>,
)
