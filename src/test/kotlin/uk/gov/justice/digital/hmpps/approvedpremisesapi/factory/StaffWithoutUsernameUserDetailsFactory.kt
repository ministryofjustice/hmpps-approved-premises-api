package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserTeamMembership
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffWithoutUsernameUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffWithoutUsernameUserDetailsFactory : Factory<StaffWithoutUsernameUserDetails> {
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var forenames: Yielded<String> = { randomStringUpperCase(8) }
  private var surname: Yielded<String> = { randomStringUpperCase(8) }
  private var teams: Yielded<List<StaffUserTeamMembership>> = { listOf() }
  private var probationAreaCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var probationAreaDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

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

  override fun produce() = StaffWithoutUsernameUserDetails(
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
