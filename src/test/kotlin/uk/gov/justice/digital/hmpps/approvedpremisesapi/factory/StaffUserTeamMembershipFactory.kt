package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.KeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserTeamMembership
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class StaffUserTeamMembershipFactory : Factory<StaffUserTeamMembership> {
  private var code: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var telephone: Yielded<String?> = { null }
  private var emailAddress: Yielded<String?> = { null }
  private var localDeliveryUnit: Yielded<KeyValue> = { randomKeyValue() }
  private var teamType: Yielded<KeyValue> = { randomKeyValue() }
  private var district: Yielded<KeyValue> = { randomKeyValue() }
  private var borough: Yielded<KeyValue> = { randomKeyValue() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(5) }
  private var endDate: Yielded<LocalDate?> = { null }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withTelephone(telephone: String?) = apply {
    this.telephone = { telephone }
  }

  fun withEmailAddress(emailAddress: String?) = apply {
    this.emailAddress = { emailAddress }
  }

  fun withLocalDeliveryUnit(localDeliveryUnit: KeyValue) = apply {
    this.localDeliveryUnit = { localDeliveryUnit }
  }

  fun withTeamType(teamType: KeyValue) = apply {
    this.teamType = { teamType }
  }

  fun withDistrict(district: KeyValue) = apply {
    this.district = { district }
  }

  fun withBorough(borough: KeyValue) = apply {
    this.borough = { borough }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  override fun produce() = StaffUserTeamMembership(
    code = this.code(),
    description = this.description(),
    telephone = this.telephone(),
    emailAddress = this.emailAddress(),
    localDeliveryUnit = this.localDeliveryUnit(),
    teamType = this.teamType(),
    district = this.district(),
    borough = this.borough(),
    startDate = this.startDate(),
    endDate = this.endDate()
  )

  private fun randomKeyValue() = KeyValue(
    code = randomStringMultiCaseWithNumbers(5),
    description = randomStringMultiCaseWithNumbers(10)
  )
}
