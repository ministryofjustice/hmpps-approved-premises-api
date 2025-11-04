package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BookingSearchResults(

  val resultsCount: kotlin.Int,

  val results: kotlin.collections.List<BookingSearchResult>,
)
