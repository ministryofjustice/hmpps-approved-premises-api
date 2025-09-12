package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate

data class Cas3PremisesArchiveAction(
  val status: Cas3PremisesStatus,
  val date: LocalDate,
)
