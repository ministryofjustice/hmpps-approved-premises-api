package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import java.time.LocalDate
import java.util.UUID

class PlacementAppealRejectedFactory : Factory<PlacementAppealRejected> {
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }
  private var rejectedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var rejectionReason: Yielded<Cas1DomainEventCodedId> = { Cas1DomainEventCodedIdFactory().produce() }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withRejectedBy(rejectedBy: StaffMember) = apply {
    this.rejectedBy = { rejectedBy }
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

  fun withRejectionReason(rejectionReason: Cas1DomainEventCodedId) = apply {
    this.rejectionReason = { rejectionReason }
  }

  override fun produce() = PlacementAppealRejected(
    bookingId = bookingId(),
    premises = premises(),
    arrivalOn = arrivalOn(),
    departureOn = departureOn(),
    rejectedBy = rejectedBy(),
    rejectionReason = rejectionReason(),
  )
}
