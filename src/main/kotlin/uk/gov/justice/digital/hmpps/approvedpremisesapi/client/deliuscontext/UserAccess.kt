package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext

data class UserAccess(
  var access: List<CaseAccess>,
)

data class CaseAccess(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String? = null,
  val restrictionMessage: String? = null,
)
