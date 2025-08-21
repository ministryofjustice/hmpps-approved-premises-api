package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.LocalDate
import java.util.UUID

data class Cas3ValidationResults(
  val items: List<Cas3ValidationResult> = emptyList(),
)

data class Cas3ValidationResult(
  val entityId: UUID,
  val entityReference: String,
  val date: LocalDate?,
)
