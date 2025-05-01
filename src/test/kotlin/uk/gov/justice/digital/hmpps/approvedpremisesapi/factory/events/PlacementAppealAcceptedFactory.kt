package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.util.UUID

class PlacementAppealAcceptedFactory : Factory<PlacementAppealAccepted> {
  private var changeRequestId = { UUID.randomUUID() }
  private var booking = { EventBookingSummaryFactory().produce() }
  private var acceptedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }

  fun withBooking(booking: EventBookingSummary) = apply {
    this.booking = { booking }
  }

  override fun produce() = PlacementAppealAccepted(
    changeRequestId = changeRequestId(),
    booking = booking(),
    acceptedBy = acceptedBy(),
  )
}
