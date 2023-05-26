package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi

data class InmateDetail(
  val offenderNo: String,
  val inOutStatus: InOutStatus,
  val assignedLivingUnit: AssignedLivingUnit?,
)

data class AssignedLivingUnit(
  val agencyId: String,
  val locationId: Long,
  val description: String?,
  val agencyName: String,
)

enum class InOutStatus {
  IN,
  OUT,
  TRN,
}
