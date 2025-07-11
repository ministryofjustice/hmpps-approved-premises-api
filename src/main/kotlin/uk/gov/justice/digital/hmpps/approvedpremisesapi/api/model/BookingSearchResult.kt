package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param person
 * @param booking
 * @param premises
 * @param room
 * @param bed
 */
data class BookingSearchResult(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: BookingSearchResultPersonSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("booking", required = true) val booking: BookingSearchResultBookingSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: BookingSearchResultPremisesSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("room", required = true) val room: BookingSearchResultRoomSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bed", required = true) val bed: BookingSearchResultBedSummary,
)
