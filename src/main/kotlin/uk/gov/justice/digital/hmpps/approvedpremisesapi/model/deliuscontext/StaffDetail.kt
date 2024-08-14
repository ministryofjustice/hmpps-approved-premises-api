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
  fun activeTeamsNewestFirst() = teams
    .filter { it.endDate == null }
    .sortedByDescending { it.startDate }

  fun teamCodes() = teams.map { it.code }
}

data class PersonName(
  val forename: String,
  val surname: String,
  val middleName: String?,
) {
  fun deliusName() = "$forename $surname"
}

data class ProbationArea(
  val code: String,
  val description: String,
)
