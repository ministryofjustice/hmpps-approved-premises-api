package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles

data class NomisUserDetail(
  val username: String,
  val staffId: Long,
  val firstName: String,
  val lastName: String,
  val activeCaseloadId: String?,
  val accountStatus: String?,
  val accountType: String,
  val primaryEmail: String?,
  val dpsRoleCodes: List<String>,
  val accountNonLocked: Boolean?,
  val credentialsNonExpired: Boolean?,
  val enabled: Boolean,
  val admin: Boolean?,
  val active: Boolean,
  val staffStatus: String?,
)
