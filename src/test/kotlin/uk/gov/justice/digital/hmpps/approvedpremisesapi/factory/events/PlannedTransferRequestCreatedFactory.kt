package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.util.UUID

class PlannedTransferRequestCreatedFactory : Factory<PlannedTransferRequestCreated> {
  private var changeRequestId = { UUID.randomUUID() }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var requestedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var reason: Yielded<Cas1DomainEventCodedId> = { Cas1DomainEventCodedIdFactory().produce() }

  fun withRequestedBy(requestedBy: StaffMember) = apply { this.requestedBy = { requestedBy } }
  fun withReason(reason: Cas1DomainEventCodedId) = apply { this.reason = { reason } }
  fun withBooking(booking: EventBookingSummary) = apply { this.booking = { booking } }

  override fun produce() = PlannedTransferRequestCreated(
    changeRequestId = changeRequestId(),
    booking = booking(),
    requestedBy = requestedBy(),
    reason = reason(),
  )
}
