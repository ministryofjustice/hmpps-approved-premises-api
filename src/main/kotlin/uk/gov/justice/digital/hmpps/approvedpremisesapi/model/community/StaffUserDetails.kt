package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import java.time.LocalDate

@Deprecated(
  message = "Deprecated as part of the move away from the community-api",
  replaceWith = ReplaceWith("StaffDetail"),
)
data class StaffUserDetails(
  val username: String,
  val email: String?,
  val telephoneNumber: String?,
  val staffCode: String,
  val staffIdentifier: Long,
  val staff: StaffNames,
  val teams: List<StaffUserTeamMembership>?,
  val probationArea: StaffProbationArea,
) {
  fun toStaffMember() = StaffMember(
    staffCode = this.staffCode,
    staffIdentifier = this.staffIdentifier,
    forenames = this.staff.forenames,
    surname = this.staff.surname,
    username = this.username,
  )
}

data class StaffWithoutUsernameUserDetails(
  val staffCode: String,
  val staffIdentifier: Long,
  val staff: StaffNames,
  val teams: List<StaffUserTeamMembership>?,
  val probationArea: StaffProbationArea?,
)

data class StaffNames(
  val forenames: String,
  val surname: String,
  val fullName: String = "$forenames $surname",
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
  val endDate: LocalDate?,
)

data class KeyValue(
  val code: String,
  val description: String,
)

data class StaffProbationArea(
  val code: String,
  val description: String,
)
