package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffMemberFactory : Factory<StaffMember> {
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 2000).toLong() }
  private var forenames: Yielded<String> = { randomStringUpperCase(6) }
  private var surname: Yielded<String> = { randomStringUpperCase(6) }

  override fun produce(): StaffMember = StaffMember(
    staffCode = this.staffCode(),
    staffIdentifier = this.staffIdentifier(),
    staff = StaffInfo(
      forenames = this.forenames(),
      surname = this.surname()
    )
  )
}
