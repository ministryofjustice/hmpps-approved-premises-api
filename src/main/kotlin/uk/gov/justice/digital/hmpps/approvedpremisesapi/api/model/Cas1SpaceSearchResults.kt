package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param resultsCount
 * @param results
 * @param searchCriteria
 */
data class Cas1SpaceSearchResults(

  @Schema(example = "4", required = true, description = "")
  @get:JsonProperty("resultsCount", required = true) val resultsCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("results", required = true) val results: kotlin.collections.List<Cas1SpaceSearchResult>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("searchCriteria") val searchCriteria: Cas1SpaceSearchParameters? = null,
)
