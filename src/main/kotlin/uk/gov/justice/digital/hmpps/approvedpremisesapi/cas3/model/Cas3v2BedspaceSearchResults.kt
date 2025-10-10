package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

data class Cas3v2BedspaceSearchResults(
  val resultsBedspaceCount: Int,
  val resultsPremisesCount: Int,
  val results: List<Cas3v2BedspaceSearchResult>,
)
