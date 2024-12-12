package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param overlaps 
 */
data class TemporaryAccommodationBedSearchResult(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("overlaps", required = true) val overlaps: kotlin.collections.List<TemporaryAccommodationBedSearchResultOverlap>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceName", required = true) override val serviceName: ServiceName,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) override val premises: BedSearchResultPremisesSummary,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("room", required = true) override val room: BedSearchResultRoomSummary,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bed", required = true) override val bed: BedSearchResultBedSummary
) : BedSearchResult{

}

