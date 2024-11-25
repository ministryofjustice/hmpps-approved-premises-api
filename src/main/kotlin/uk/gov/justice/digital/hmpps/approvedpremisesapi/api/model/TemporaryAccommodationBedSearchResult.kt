package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
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
  @get:JsonProperty("bed", required = true) override val bed: BedSearchResultBedSummary,
) : BedSearchResult {
}
