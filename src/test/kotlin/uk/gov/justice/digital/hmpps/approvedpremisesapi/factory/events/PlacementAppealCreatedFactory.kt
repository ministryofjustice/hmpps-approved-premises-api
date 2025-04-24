package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.time.LocalDate
import java.util.UUID

class PlacementAppealCreatedFactory : Factory<PlacementAppealCreated> {
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }
  private var requestedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var appealReason: Yielded<Cas1DomainEventCodedId> = { Cas1DomainEventCodedIdFactory().produce() }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withRequestedBy(requestedBy: StaffMember) = apply {
    this.requestedBy = { requestedBy }
  }

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withAppealReason(appealReason: Cas1DomainEventCodedId) = apply {
    this.appealReason = { appealReason }
  }

  fun withArrivalOn(arrivalOn: LocalDate) = apply {
    this.arrivalOn = { arrivalOn }
  }

  fun withDepartureOn(departureOn: LocalDate) = apply {
    this.departureOn = { departureOn }
  }

  override fun produce(): PlacementAppealCreated = PlacementAppealCreated(
    bookingId = bookingId(),
    premises = premises(),
    arrivalOn = arrivalOn(),
    departureOn = departureOn(),
    requestedBy = requestedBy(),
    appealReason = appealReason(),
  )
}
