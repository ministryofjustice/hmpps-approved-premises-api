package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class NomisUserDetailFactory : Factory<NomisUserDetail> {
  private var username: Yielded<String> = { randomStringUpperCase(8) }
  private var staffId: Yielded<Long> = { randomInt(100000, 900000).toLong() }
  private var firstName: Yielded<String> = { randomStringUpperCase(8) }
  private var lastName: Yielded<String> = { randomStringUpperCase(8) }
  private var accountType: Yielded<String> = { "OPEN" }
  private var dpsRoleCodes: Yielded<List<String>> = { listOf() }
  private var enabled: Yielded<Boolean> = { true }
  private var active: Yielded<Boolean> = { true }

  private var activeCaseloadId: Yielded<String?> = { randomStringUpperCase(6) }
  private var primaryEmail: Yielded<String?> = { randomStringUpperCase(8) }
  private var accountStatus: Yielded<String?> = { "OPEN" }
  private var accountNonLocked: Yielded<Boolean> = { true }
  private var credentialsNonExpired: Yielded<Boolean> = { true }
  private var admin: Yielded<Boolean> = { false }
  private var staffStatus: Yielded<String?> = { "ACTIVE" }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withFirstName(firstName: String) = apply {
    this.firstName = { firstName }
  }

  fun withLastName(lastName: String) = apply {
    this.lastName = { lastName }
  }

  fun withStaffId(staffId: Long) = apply {
    this.staffId = { staffId }
  }

  fun withAccountType(accountType: String) = apply {
    this.accountType = { accountType }
  }

  fun withEmail(email: String) = apply {
    this.primaryEmail = { email }
  }

  fun withEnabled(enabled: Boolean) = apply {
    this.enabled = { enabled }
  }

  fun withActive(active: Boolean) = apply {
    this.active = { active }
  }

  fun withActiveCaseloadId(activeCaseloadId: String) = apply {
    this.activeCaseloadId = { activeCaseloadId }
  }

  override fun produce(): NomisUserDetail = NomisUserDetail(
    username = this.username(),
    staffId = this.staffId(),
    firstName = this.firstName(),
    lastName = this.lastName(),
    accountType = this.accountType(),
    dpsRoleCodes = this.dpsRoleCodes(),
    enabled = this.enabled(),
    active = this.active(),

    activeCaseloadId = this.activeCaseloadId(),
    primaryEmail = this.primaryEmail(),
    accountStatus = this.accountStatus(),
    accountNonLocked = this.accountNonLocked(),
    credentialsNonExpired = this.credentialsNonExpired(),
    admin = this.admin(),
    staffStatus = this.staffStatus(),
  )
}
