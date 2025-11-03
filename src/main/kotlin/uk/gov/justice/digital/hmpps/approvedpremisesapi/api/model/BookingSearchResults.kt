package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param resultsCount
 * @param results
 */
data class BookingSearchResults(

  @get:JsonProperty("resultsCount", required = true) val resultsCount: kotlin.Int,

  @get:JsonProperty("results", required = true) val results: kotlin.collections.List<BookingSearchResult>,
)
