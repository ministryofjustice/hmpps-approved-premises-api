package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

data class UserAccess(
  var access: List<CaseAccess>,
)

data class CaseAccess(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String?,
  val restrictionMessage: String?,
) {
  fun isLao() = userExcluded || userRestricted
}
