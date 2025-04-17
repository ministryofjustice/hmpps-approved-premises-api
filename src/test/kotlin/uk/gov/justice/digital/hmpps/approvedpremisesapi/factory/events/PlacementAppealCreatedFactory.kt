package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated
import java.time.LocalDate
import java.util.UUID

class PlacementAppealCreatedFactory : Factory<PlacementAppealCreated> {
  override fun produce(): PlacementAppealCreated = PlacementAppealCreated(
    bookingId = UUID.randomUUID(),
    premises = EventPremisesFactory().produce(),
    arrivalOn = LocalDate.now(),
    departureOn = LocalDate.now(),
    requestedBy = StaffMemberFactory().produce(),
    appealReason = Cas1DomainEventNamedIdFactory().produce(),
  )
}
