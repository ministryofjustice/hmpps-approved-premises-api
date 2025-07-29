package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesStatus
import java.util.UUID

data class Cas3PremisesBedspaceTotals(
  val id: UUID,
  val status: Cas3PremisesStatus,
  val totalOnlineBedspaces: Int,
  val totalUpcomingBedspaces: Int,
  val totalArchivedBedspaces: Int,
)
