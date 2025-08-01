package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate

data class Cas3BedspaceArchiveAction(
  val status: Cas3BedspaceStatus,
  val date: LocalDate,
)
