package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param resultsCount
 * @param results
 */
data class BookingSearchResults(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("resultsCount", required = true) val resultsCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("results", required = true) val results: kotlin.collections.List<BookingSearchResult>,
)
