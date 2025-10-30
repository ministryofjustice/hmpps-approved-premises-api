package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.events.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffMemberFactory : Factory<Cas2StaffMember> {
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 5000).toLong() }
  private var name: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(8) }

  fun withStaffIdentifier(id: Long) = apply {
    this.staffIdentifier = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): Cas2StaffMember = Cas2StaffMember(
    staffIdentifier = this.staffIdentifier(),
    name = this.name(),
    username = this.username(),
  )
}
