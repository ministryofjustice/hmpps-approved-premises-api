package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ContextStaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ContextStaffMemberName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class ContextStaffMemberFactory : Factory<ContextStaffMember> {
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var forenames: Yielded<String> = { randomStringUpperCase(6) }
  private var surname: Yielded<String> = { randomStringUpperCase(6) }

  override fun produce(): ContextStaffMember = ContextStaffMember(
    code = this.staffCode(),
    keyWorker = false,
    name = ContextStaffMemberName(
      forename = this.forenames(),
      middleName = null,
      surname = this.surname()
    )
  )
}
