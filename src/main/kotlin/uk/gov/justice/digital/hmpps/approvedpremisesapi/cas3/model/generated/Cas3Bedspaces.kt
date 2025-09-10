package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace

data class Cas3Bedspaces(
  val bedspaces: List<Cas3Bedspace>,
  val totalOnlineBedspaces: Int,
  val totalUpcomingBedspaces: Int,
  val totalArchivedBedspaces: Int,
)
