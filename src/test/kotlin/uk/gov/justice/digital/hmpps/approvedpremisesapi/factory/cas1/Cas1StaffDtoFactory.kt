package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class Cas1StaffDtoFactory : Factory<Cas1StaffDto> {
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var username: Yielded<String> = { randomStringLowerCase(8) }
  private var staffCode: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  override fun produce() = Cas1StaffDto(
    name = this.name(),
    username = this.username(),
    staffCode = this.staffCode(),
  )
}
