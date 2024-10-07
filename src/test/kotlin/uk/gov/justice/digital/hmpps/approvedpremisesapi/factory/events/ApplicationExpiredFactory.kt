package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationExpired
import java.util.UUID

class ApplicationExpiredFactory : Factory<ApplicationExpired> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  override fun produce() = ApplicationExpired(
    applicationId = this.applicationId(),
  )
}
