package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BookingSearchResults(

  @get:JsonProperty("resultsCount", required = true) val resultsCount: Int,

  @get:JsonProperty("results", required = true) val results: List<BookingSearchResult>,
)
