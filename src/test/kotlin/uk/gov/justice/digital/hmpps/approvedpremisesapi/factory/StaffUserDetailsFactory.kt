package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserTeamMembership
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

@Deprecated(message = "StaffUserDetails are being removed with the community api", replaceWith = ReplaceWith("StaffDetailFactory"))
class StaffUserDetailsFactory : Factory<StaffUserDetails> {
  private var username: Yielded<String> = { randomStringUpperCase(10) }
  private var email: Yielded<String?> = { randomStringUpperCase(8) }
  private var telephoneNumber: Yielded<String?> = { randomStringUpperCase(8) }
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var forenames: Yielded<String> = { randomStringUpperCase(8) }
  private var surname: Yielded<String> = { randomStringUpperCase(8) }
  private var teams: Yielded<List<StaffUserTeamMembership>> = { listOf() }
  private var probationAreaCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var probationAreaDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  fun withoutEmail() = apply {
    this.email = { null }
  }

  fun withTelephoneNumber(telephoneNumber: String?) = apply {
    this.telephoneNumber = { telephoneNumber }
  }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  fun withStaffIdentifier(staffIdentifier: Long) = apply {
    this.staffIdentifier = { staffIdentifier }
  }

  fun withForenames(forenames: String) = apply {
    this.forenames = { forenames }
  }

  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }

  fun withTeams(teams: List<StaffUserTeamMembership>) = apply {
    this.teams = { teams }
  }

  fun withProbationAreaCode(probationAreaCode: String) = apply {
    this.probationAreaCode = { probationAreaCode }
  }

  fun withProbationAreaDescription(probationAreaDescription: String) = apply {
    this.probationAreaDescription = { probationAreaDescription }
  }

  override fun produce(): StaffUserDetails = StaffUserDetails(
    username = this.username(),
    email = this.email(),
    telephoneNumber = this.telephoneNumber(),
    staffCode = this.staffCode(),
    staffIdentifier = this.staffIdentifier(),
    staff = StaffNames(
      forenames = this.forenames(),
      surname = this.surname(),
    ),
    teams = this.teams(),
    probationArea = StaffProbationArea(
      code = this.probationAreaCode(),
      description = this.probationAreaDescription(),
    ),
  )
}

fun StaffUserDetails.toStaffDetail(): StaffDetail {
  return StaffDetail(
    email,
    telephoneNumber,
    staffIdentifier,
    teams?.let {
      it.map { team ->
        Team(
          team.code,
          team.description,
          Ldu(team.localDeliveryUnit.code, team.localDeliveryUnit.description),
          Borough(team.borough.code, team.borough.description),
          team.startDate,
          team.endDate,
        )
      }
    } ?: emptyList(),
    ProbationArea(probationArea.code, probationArea.description),
    username,
    PersonName(staff.forenames, staff.surname, ""),
    staffCode,
    true,
  )
}
