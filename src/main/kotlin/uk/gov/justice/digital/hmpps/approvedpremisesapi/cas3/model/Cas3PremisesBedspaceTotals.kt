package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate
import java.util.UUID

data class Cas3PremisesBedspaceTotals(
  val id: UUID,
  val status: Cas3PremisesStatus,
  val premisesEndDate: LocalDate?,
  val totalOnlineBedspaces: Int,
  val totalUpcomingBedspaces: Int,
  val totalArchivedBedspaces: Int,
)
