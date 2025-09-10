package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate
import java.util.UUID

data class Cas3BedspaceSummary(
  val id: UUID,
  val reference: String,
  val endDate: LocalDate? = null,
)
