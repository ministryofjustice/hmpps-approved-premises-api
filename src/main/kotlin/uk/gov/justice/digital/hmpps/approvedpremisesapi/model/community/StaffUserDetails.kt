package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate

data class StaffUserDetails(
  val username: String,
  val email: String,
  val telephoneNumber: String?,
  val staffCode: String,
  val staffIdentifier: Long,
  val staff: StaffNames,
  val teams: List<StaffUserTeamMembership>
)

data class StaffNames(
  val forenames: String,
  val surname: String
)

data class StaffUserTeamMembership(
  val code: String,
  val description: String,
  val telephone: String?,
  val emailAddress: String?,
  val localDeliveryUnit: KeyValue,
  val teamType: KeyValue,
  val district: KeyValue,
  val borough: KeyValue,
  val startDate: LocalDate,
  val endDate: LocalDate?
)

data class KeyValue(
  val code: String,
  val description: String
)
