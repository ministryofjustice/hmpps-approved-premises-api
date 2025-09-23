package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

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

  val costCentre: String? = null,
)
