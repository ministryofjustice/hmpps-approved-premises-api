package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre

data class NewLostBed(
  val startDate: java.time.LocalDate,
  val endDate: java.time.LocalDate,
  val reason: java.util.UUID,
  val bedId: java.util.UUID,
  val referenceNumber: kotlin.String? = null,
  val notes: kotlin.String? = null,
  val costCentre: Cas3CostCentre? = null,
)
