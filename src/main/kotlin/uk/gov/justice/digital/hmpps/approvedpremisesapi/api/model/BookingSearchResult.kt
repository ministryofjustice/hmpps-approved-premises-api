package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BookingSearchResult(

  val person: BookingSearchResultPersonSummary,

  val booking: BookingSearchResultBookingSummary,

  val premises: BookingSearchResultPremisesSummary,

  val room: BookingSearchResultRoomSummary,

  val bed: BookingSearchResultBedSummary,
)
