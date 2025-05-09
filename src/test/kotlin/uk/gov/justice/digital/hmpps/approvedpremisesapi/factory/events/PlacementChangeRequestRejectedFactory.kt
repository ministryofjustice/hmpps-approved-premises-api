package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected
import java.util.UUID

class PlacementChangeRequestRejectedFactory : Factory<PlacementChangeRequestRejected> {
  private var changeRequestId = { UUID.randomUUID() }
  private var changeRequestType = { EventChangeRequestType.PLACEMENT_APPEAL }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var rejectedBy = { StaffMemberFactory().produce() }
  private var reason = { Cas1DomainEventCodedIdFactory().produce() }

  fun withChangeRequestType(changeRequestType: EventChangeRequestType) = apply {
    this.changeRequestType = { changeRequestType }
  }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  fun withReason(reason: Cas1DomainEventCodedId) = apply {
    this.reason = { reason }
  }

  override fun produce() = PlacementChangeRequestRejected(
    changeRequestId = changeRequestId(),
    changeRequestType = changeRequestType(),
    booking = booking(),
    rejectedBy = rejectedBy(),
    reason = reason(),
  )
}
