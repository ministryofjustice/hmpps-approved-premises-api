package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

data class StaffMembersPage(
  val content: List<ContextStaffMember>
)

data class ContextStaffMember(
  val code: String,
  val keyWorker: Boolean,
  val name: ContextStaffMemberName
)

data class ContextStaffMemberName(
  val forename: String,
  val middleName: String?,
  val surname: String
)
