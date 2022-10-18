package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffUserDetailsFactory : Factory<StaffUserDetails> {
  private var username: Yielded<String> = { randomStringUpperCase(10) }
  private var email: Yielded<String> = { randomStringUpperCase(8) }
  private var telephoneNumber: Yielded<String> = { randomStringUpperCase(8) }
  private var staffCode: Yielded<String> = { randomStringUpperCase(8) }
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var forenames: Yielded<String> = { randomStringUpperCase(8) }
  private var surname: Yielded<String> = { randomStringUpperCase(8) }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): StaffUserDetails = StaffUserDetails(
    username = this.username(),
    email = this.email(),
    telephoneNumber = this.telephoneNumber(),
    staffCode = this.staffCode(),
    staffIdentifier = this.staffIdentifier(),
    staff = StaffNames(
      forenames = this.forenames(),
      surname = this.surname()
    )
  )
}
