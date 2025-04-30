package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected

class PlacementAppealRejectedFactory : Factory<PlacementAppealRejected> {
  private var booking = { EventBookingSummaryFactory().produce() }
  private var rejectedBy = { StaffMemberFactory().produce() }
  private var reason = { Cas1DomainEventCodedIdFactory().produce() }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  fun withReason(reason: Cas1DomainEventCodedId) = apply {
    this.reason = { reason }
  }

  override fun produce() = PlacementAppealRejected(
    booking = booking(),
    rejectedBy = rejectedBy(),
    reason = reason(),
  )
}
