package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferType
import java.util.UUID

class EventTransferInfoFactory : Factory<EventTransferInfo> {
  private var type = { EventTransferType.PLANNED }
  private var changeRequestId = { UUID.randomUUID() }
  private var booking = { EventBookingSummaryFactory().produce() }

  fun withType(type: EventTransferType) = apply {
    this.type = { type }
  }

  fun withChangeRequestId(changeRequestId: UUID) = apply {
    this.changeRequestId = { changeRequestId }
  }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  override fun produce() = EventTransferInfo(
    type = type(),
    changeRequestId = changeRequestId(),
    booking = booking(),
  )
}
