package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class ContextStaffMemberFactory : Factory<StaffMember> {
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var forenames: Yielded<String> = { randomStringUpperCase(6) }
  private var surname: Yielded<String> = { randomStringUpperCase(6) }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  fun withForenames(forenames: String) = apply {
    this.forenames = { forenames }
  }

  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }

  override fun produce(): StaffMember = StaffMember(
    code = this.staffCode(),
    keyWorker = false,
    name = PersonName(
      forename = this.forenames(),
      middleName = null,
      surname = this.surname(),
    ),
  )
}
