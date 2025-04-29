package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.util.UUID

class PlannedTransferRequestAcceptedFactory : Factory<PlannedTransferRequestAccepted> {
  private var changeRequestId = { UUID.randomUUID() }
  private var acceptedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var from = { EventBookingSummaryFactory().produce() }
  private var to = { EventBookingSummaryFactory().produce() }

  fun withAcceptedBy(acceptedBy: StaffMember) = apply { this.acceptedBy = { acceptedBy } }
  fun withFrom(from: EventBookingSummary) = apply { this.from = { from } }
  fun withTo(to: EventBookingSummary) = apply { this.to = { to } }

  override fun produce() = PlannedTransferRequestAccepted(
    changeRequestId = changeRequestId(),
    acceptedBy = acceptedBy(),
    from = from(),
    to = to(),
  )
}
