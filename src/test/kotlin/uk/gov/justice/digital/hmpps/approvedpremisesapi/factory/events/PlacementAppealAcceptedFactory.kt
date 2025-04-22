package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.time.LocalDate
import java.util.UUID

class PlacementAppealAcceptedFactory : Factory<PlacementAppealAccepted> {
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }
  private var acceptedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withAcceptedBy(acceptedBy: StaffMember) = apply {
    this.acceptedBy = { acceptedBy }
  }

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withArrivalOn(arrivalOn: LocalDate) = apply {
    this.arrivalOn = { arrivalOn }
  }

  fun withDepartureOn(departureOn: LocalDate) = apply {
    this.departureOn = { departureOn }
  }

  override fun produce() = PlacementAppealAccepted(
    bookingId = bookingId(),
    premises = premises(),
    arrivalOn = arrivalOn(),
    departureOn = departureOn(),
    acceptedBy = acceptedBy(),
  )
}
