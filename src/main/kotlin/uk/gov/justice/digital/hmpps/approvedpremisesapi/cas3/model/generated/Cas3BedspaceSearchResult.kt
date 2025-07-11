package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary

/**
 *
 * @param premises
 * @param room
 * @param bed
 * @param overlaps
 */
data class Cas3BedspaceSearchResult(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: BedSearchResultPremisesSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("room", required = true) val room: BedSearchResultRoomSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bed", required = true) val bed: BedSearchResultBedSummary,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("overlaps", required = true) val overlaps: List<Cas3BedspaceSearchResultOverlap>,
)
