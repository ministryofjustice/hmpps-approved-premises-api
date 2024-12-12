package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultRoomSummary
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
    @get:JsonProperty("bed", required = true) val bed: BookingSearchResultBedSummary
) {

}

