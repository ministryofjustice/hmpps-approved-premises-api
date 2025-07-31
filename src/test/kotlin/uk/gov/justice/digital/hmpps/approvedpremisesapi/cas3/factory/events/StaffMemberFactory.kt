package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffMemberFactory : Factory<StaffMember> {
  private var staffCode: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(10) }
  private var probationRegionCode: Yielded<String> = { randomStringUpperCase(10) }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withProbationRegionCode(probationRegionCode: String) = apply {
    this.probationRegionCode = { probationRegionCode }
  }

  override fun produce() = StaffMember(
    staffCode = this.staffCode(),
    username = this.username(),
    probationRegionCode = this.probationRegionCode(),
  )
}
