package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate
import java.util.UUID

data class UpdateLostBed(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reason: UUID,
  val referenceNumber: String? = null,
  val notes: String? = null,
)
