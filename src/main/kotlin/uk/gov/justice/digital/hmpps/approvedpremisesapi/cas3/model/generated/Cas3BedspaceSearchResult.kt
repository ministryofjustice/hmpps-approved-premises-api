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

  val premises: BedSearchResultPremisesSummary,

  val room: BedSearchResultRoomSummary,

  val bed: BedSearchResultBedSummary,

  val overlaps: List<Cas3BedspaceSearchResultOverlap>,
)
