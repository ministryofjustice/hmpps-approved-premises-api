package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre

data class LostBed(

  val id: java.util.UUID,

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  val bedId: java.util.UUID,

  val bedName: String,

  val roomName: String,

  val reason: LostBedReason,

  val status: LostBedStatus,

  val referenceNumber: String? = null,

  val notes: String? = null,

  val cancellation: LostBedCancellation? = null,

  val costCentre: Cas3CostCentre? = null,
)
