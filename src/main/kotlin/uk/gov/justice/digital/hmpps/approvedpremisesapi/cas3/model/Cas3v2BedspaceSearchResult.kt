package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

data class Cas3v2BedspaceSearchResult(
  val premises: Cas3BedspaceSearchResultPremisesSummary,
  val bedspace: Cas3BedspaceSearchResultBedspaceSummary,
  val overlaps: List<Cas3v2BedspaceSearchResultOverlap>,
)
