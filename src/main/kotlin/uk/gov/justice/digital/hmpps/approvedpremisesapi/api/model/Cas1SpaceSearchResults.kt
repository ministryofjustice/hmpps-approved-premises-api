package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param resultsCount
 * @param results
 */
data class Cas1SpaceSearchResults(

  @Schema(example = "4", required = true, description = "")
  val resultsCount: kotlin.Int,

  val results: kotlin.collections.List<Cas1SpaceSearchResult>,
)
