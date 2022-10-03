package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

data class StaffMember(
  val staffCode: String,
  val staffIdentifier: Long,
  val staff: StaffInfo
)

data class StaffInfo(
  val forenames: String,
  val surname: String
)
