package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated
import java.util.UUID

class PlacementChangeRequestCreatedFactory : Factory<PlacementChangeRequestCreated> {
  private var changeRequestId = { UUID.randomUUID() }
  private var changeRequestType = { EventChangeRequestType.PLACEMENT_APPEAL }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var requestedBy = { StaffMemberFactory().produce() }
  private var reason = { Cas1DomainEventCodedIdFactory().produce() }

  fun withChangeRequestId(changeRequestId: UUID) = apply {
    this.changeRequestId = { changeRequestId }
  }

  fun withChangeRequestType(changeRequestType: EventChangeRequestType) = apply {
    this.changeRequestType = { changeRequestType }
  }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  fun withReason(reason: Cas1DomainEventCodedId) = apply {
    this.reason = { reason }
  }

  override fun produce(): PlacementChangeRequestCreated = PlacementChangeRequestCreated(
    changeRequestId = changeRequestId(),
    changeRequestType = changeRequestType(),
    booking = booking(),
    requestedBy = requestedBy(),
    reason = reason(),
  )
}
