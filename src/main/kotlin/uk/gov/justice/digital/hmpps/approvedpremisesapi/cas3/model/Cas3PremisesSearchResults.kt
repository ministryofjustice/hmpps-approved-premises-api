package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResult

data class Cas3PremisesSearchResults(
  val totalPremises: Int,
  val results: List<Cas3PremisesSearchResult>? = null,
  val totalOnlineBedspaces: Int? = null,
  val totalUpcomingBedspaces: Int? = null,
)
