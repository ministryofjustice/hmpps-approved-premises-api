package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredManually
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class ApplicationExpiredManuallyFactory : Factory<ApplicationExpiredManually> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var expiredBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var expiredAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var expiryReason: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withExpiryReason(expiryReason: String) = apply {
    this.expiryReason = { expiryReason }
  }

  override fun produce() = ApplicationExpiredManually(
    applicationId = this.applicationId(),
    expiredBy = this.expiredBy(),
    expiredAt = this.expiredAt(),
    expiredReason = this.expiryReason(),
  )
}
