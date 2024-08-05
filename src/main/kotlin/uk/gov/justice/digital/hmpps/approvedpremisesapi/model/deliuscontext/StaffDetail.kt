package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

import org.apache.commons.collections4.CollectionUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

data class StaffDetail(
  val email: String?,
  val telephoneNumber: String?,
  val staffIdentifier: Long,
  val teams: List<Team> = emptyList(),
  val probationArea: ProbationArea,
  val username: String,
  val name: PersonName,
  val code: String,
  val active: Boolean,
) {
  fun getTeamCodes() = teams.map { it.code }

  fun getActiveTeams() =
    teams
      .filter { it.endDate != null }
      .sortedByDescending { it.startDate }

  fun isUpdated(user: UserEntity): Boolean =
    (email != user.email) ||
      (telephoneNumber != user.telephoneNumber) ||
      (name.toFullName() != user.name) ||
      (code != user.deliusStaffCode) ||
      (probationArea.code != user.probationRegion.deliusCode) ||
      !CollectionUtils.isEqualCollection(getTeamCodes(), user.teamCodes)
}

data class PersonName(
  val forename: String,
  val surname: String,
  val middleName: String?,
) {
  fun toFullName() = "$forename $surname"
}

data class ProbationArea(
  val code: String,
  val description: String,
)
