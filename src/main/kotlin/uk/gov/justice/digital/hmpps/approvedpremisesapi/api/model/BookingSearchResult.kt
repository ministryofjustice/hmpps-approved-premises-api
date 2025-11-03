package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param person
 * @param booking
 * @param premises
 * @param room
 * @param bed
 */
data class BookingSearchResult(

  @get:JsonProperty("person", required = true) val person: BookingSearchResultPersonSummary,

  @get:JsonProperty("booking", required = true) val booking: BookingSearchResultBookingSummary,

  @get:JsonProperty("premises", required = true) val premises: BookingSearchResultPremisesSummary,

  @get:JsonProperty("room", required = true) val room: BookingSearchResultRoomSummary,

  @get:JsonProperty("bed", required = true) val bed: BookingSearchResultBedSummary,
)
