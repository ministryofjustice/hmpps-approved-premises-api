package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

data class StaffUserDetails(
  val username: String,
  val email: String,
  val telephoneNumber: String?,
  val staffCode: String,
  val staffIdentifier: Long,
  val staff: StaffNames
)

data class StaffNames(
  val forenames: String,
  val surname: String
)
