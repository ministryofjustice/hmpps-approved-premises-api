package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import java.time.LocalDate
import java.util.UUID

data class UpdateLostBed(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reason: UUID,
  val referenceNumber: String? = null,
  val notes: String? = null,
  val costCentre: Cas3CostCentre? = null,
)
