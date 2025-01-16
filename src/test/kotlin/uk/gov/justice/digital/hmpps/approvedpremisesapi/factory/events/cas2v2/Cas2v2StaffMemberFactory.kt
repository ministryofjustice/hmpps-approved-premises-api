package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.Cas2v2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class Cas2v2StaffMemberFactory : Factory<Cas2v2StaffMember> {
  private var staffCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var forenames: Yielded<String> = { randomStringUpperCase(6) }
  private var surname: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(8) }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  fun withForenames(forenames: String) = apply {
    this.forenames = { forenames }
  }

  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): Cas2v2StaffMember = Cas2v2StaffMember(
    staffIdentifier = this.staffCode(),
    name = "$forenames $surname",
    username = this.username(),
  )
}
