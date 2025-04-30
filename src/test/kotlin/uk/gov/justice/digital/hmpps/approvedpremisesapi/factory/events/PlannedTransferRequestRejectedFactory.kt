package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.util.UUID

class PlannedTransferRequestRejectedFactory : Factory<PlannedTransferRequestRejected> {
  private var changeRequestId = { UUID.randomUUID() }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var rejectedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var reason: Yielded<Cas1DomainEventCodedId> = { Cas1DomainEventCodedIdFactory().produce() }

  fun withRejectedBy(rejectedBy: StaffMember) = apply { this.rejectedBy = { rejectedBy } }
  fun withReason(reason: Cas1DomainEventCodedId) = apply { this.reason = { reason } }
  fun withBooking(booking: EventBookingSummary) = apply { this.booking = { booking } }

  override fun produce() = PlannedTransferRequestRejected(
    changeRequestId = changeRequestId(),
    booking = booking(),
    rejectedBy = rejectedBy(),
    reason = reason(),
  )
}
