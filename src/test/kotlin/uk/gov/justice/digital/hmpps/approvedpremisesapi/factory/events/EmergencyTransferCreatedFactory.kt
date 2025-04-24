package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EmergencyTransferCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.TransferBooking
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class EmergencyTransferCreatedFactory : Factory<EmergencyTransferCreated> {
  private var applicationId = { UUID.randomUUID() }
  private var createdAt = { Instant.now() }
  private var createdBy = { StaffMemberFactory().produce() }
  private var from = { TransferBookingFactory().produce() }
  private var to = { TransferBookingFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply { this.applicationId = { applicationId } }
  fun withCreatedAt(createdAt: Instant) = apply { this.createdAt = { createdAt } }
  fun withCreatedBy(createdBy: StaffMember) = apply { this.createdBy = { createdBy } }
  fun withFrom(from: TransferBooking) = apply { this.from = { from } }
  fun withTo(to: TransferBooking) = apply { this.to = { to } }

  override fun produce() = EmergencyTransferCreated(
    applicationId = applicationId(),
    createdAt = createdAt(),
    createdBy = createdBy(),
    from = from(),
    to = to(),
  )
}

class TransferBookingFactory : Factory<TransferBooking> {
  private var bookingId = { UUID.randomUUID() }
  private var premises = { EventPremisesFactory().produce() }
  private var arrivalOn = { LocalDate.now() }
  private var departureOn = { LocalDate.now() }

  fun withBookingId(bookingId: UUID) = apply { this.bookingId = { bookingId } }
  fun withPremises(premises: Premises) = apply { this.premises = { premises } }
  fun withArrivalOn(arrivalOn: LocalDate) = apply { this.arrivalOn = { arrivalOn } }
  fun withDepartureOn(departureOn: LocalDate) = apply { this.departureOn = { departureOn } }

  override fun produce() = TransferBooking(
    bookingId = bookingId(),
    premises = premises(),
    arrivalOn = arrivalOn(),
    departureOn = departureOn(),
  )
}
