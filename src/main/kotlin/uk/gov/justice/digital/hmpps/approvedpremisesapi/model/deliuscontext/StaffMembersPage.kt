package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

data class StaffMembersPage(
  val content: List<StaffMember>
)

data class StaffMember(
  val code: String,
  val keyWorker: Boolean,
  val name: StaffMemberName
)

data class StaffMemberName(
  val forename: String,
  val middleName: String?,
  val surname: String
)
