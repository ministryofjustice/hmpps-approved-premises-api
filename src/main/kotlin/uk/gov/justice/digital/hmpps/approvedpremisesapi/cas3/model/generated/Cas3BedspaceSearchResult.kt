package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
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

  @get:JsonProperty("premises", required = true) val premises: BedSearchResultPremisesSummary,

  @get:JsonProperty("room", required = true) val room: BedSearchResultRoomSummary,

  @get:JsonProperty("bed", required = true) val bed: BedSearchResultBedSummary,

  @get:JsonProperty("overlaps", required = true) val overlaps: List<Cas3BedspaceSearchResultOverlap>,
)
