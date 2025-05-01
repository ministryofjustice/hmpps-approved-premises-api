package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.util.UUID

class PlacementChangeRequestAcceptedFactory : Factory<PlacementChangeRequestAccepted> {
  private var changeRequestId = { UUID.randomUUID() }
  private var changeRequestType = { EventChangeRequestType.PLACEMENT_APPEAL }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var acceptedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  override fun produce() = PlacementChangeRequestAccepted(
    changeRequestId = changeRequestId(),
    changeRequestType = changeRequestType(),
    booking = booking(),
    acceptedBy = acceptedBy(),
  )
}
