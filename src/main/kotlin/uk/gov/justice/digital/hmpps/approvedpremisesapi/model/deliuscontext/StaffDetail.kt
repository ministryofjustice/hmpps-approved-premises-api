package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

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
}

data class PersonName(
  val forename: String,
  val surname: String,
  val middleName: String?,
) {
  fun toFullName() = this.forename + " " + this.surname
}

data class ProbationArea(
  val code: String,
  val description: String,
)
