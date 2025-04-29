package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EmergencyTransferCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.time.Instant
import java.util.UUID

class EmergencyTransferCreatedFactory : Factory<EmergencyTransferCreated> {
  private var applicationId = { UUID.randomUUID() }
  private var createdAt = { Instant.now() }
  private var createdBy = { StaffMemberFactory().produce() }
  private var from = { EventBookingSummaryFactory().produce() }
  private var to = { EventBookingSummaryFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply { this.applicationId = { applicationId } }
  fun withCreatedAt(createdAt: Instant) = apply { this.createdAt = { createdAt } }
  fun withCreatedBy(createdBy: StaffMember) = apply { this.createdBy = { createdBy } }
  fun withFrom(from: EventBookingSummary) = apply { this.from = { from } }
  fun withTo(to: EventBookingSummary) = apply { this.to = { to } }

  override fun produce() = EmergencyTransferCreated(
    applicationId = applicationId(),
    createdAt = createdAt(),
    createdBy = createdBy(),
    from = from(),
    to = to(),
  )
}
