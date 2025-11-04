package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param person
 * @param booking
 * @param premises
 * @param room
 * @param bed
 */
data class BookingSearchResult(

  val person: BookingSearchResultPersonSummary,

  val booking: BookingSearchResultBookingSummary,

  val premises: BookingSearchResultPremisesSummary,

  val room: BookingSearchResultRoomSummary,

  val bed: BookingSearchResultBedSummary,
)
