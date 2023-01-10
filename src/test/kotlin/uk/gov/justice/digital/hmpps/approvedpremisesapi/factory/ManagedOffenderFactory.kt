package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.ManagedOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

class ManagedOffenderFactory : Factory<ManagedOffender> {
  private var offenderCrn: Yielded<String> = { randomStringUpperCase(6) }
  private var allocationDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(8) }
  private var staffIdentifier: Yielded<Long> = { randomInt(1000, 2000).toLong() }
  private var teamIdentifier: Yielded<Long> = { randomInt(1000, 2000).toLong() }

  fun withOffenderCrn(offenderCrn: String) = apply {
    this.offenderCrn = { offenderCrn }
  }

  fun withAllocationDate(allocationDate: LocalDate) = apply {
    this.allocationDate = { allocationDate }
  }

  fun withStaffIdentifier(staffIdentifier: Long) = apply {
    this.staffIdentifier = { staffIdentifier }
  }

  fun withTeamIdentifier(teamIdentifier: Long) = apply {
    this.teamIdentifier = { teamIdentifier }
  }

  override fun produce(): ManagedOffender = ManagedOffender(
    offenderCrn = this.offenderCrn(),
    allocationDate = this.allocationDate(),
    staffIdentifier = this.staffIdentifier(),
    teamIdentifier = this.teamIdentifier()
  )
}
