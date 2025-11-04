package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param resultsCount
 * @param results
 */
data class BookingSearchResults(

  val resultsCount: kotlin.Int,

  val results: kotlin.collections.List<BookingSearchResult>,
)
