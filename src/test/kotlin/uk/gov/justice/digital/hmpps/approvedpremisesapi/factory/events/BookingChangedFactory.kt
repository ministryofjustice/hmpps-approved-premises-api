package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BookingChangedFactory : Factory<BookingChanged> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var changedAt: Yielded<Instant> = { Instant.now() }
  private var changedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }
  private var previousArrivalOn: Yielded<LocalDate?> = { null }
  private var previousDepartureOn: Yielded<LocalDate?> = { null }
  private var characteristics: Yielded<List<SpaceCharacteristic>?> = { null }
  private var previousCharacteristics: Yielded<List<SpaceCharacteristic>?> = { null }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withChangedAt(createdAt: Instant) = apply {
    this.changedAt = { createdAt }
  }

  fun withChangedBy(changedBy: StaffMember) = apply {
    this.changedBy = { changedBy }
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

  fun withPreviousArrivalOn(previousArrivalOn: LocalDate?) = apply {
    this.previousArrivalOn = { previousArrivalOn }
  }

  fun withPreviousDepartureOn(previousDepartureOn: LocalDate?) = apply {
    this.previousDepartureOn = { previousDepartureOn }
  }

  fun withCharacteristics(characteristics: List<SpaceCharacteristic>) = apply {
    this.characteristics = { characteristics }
  }

  fun withPreviousCharacteristics(previousCharacteristics: List<SpaceCharacteristic>?) = apply {
    this.previousCharacteristics = { previousCharacteristics }
  }

  override fun produce() = BookingChanged(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    changedAt = this.changedAt(),
    changedBy = this.changedBy(),
    premises = this.premises(),
    arrivalOn = this.arrivalOn(),
    departureOn = this.departureOn(),
    previousArrivalOn = this.previousArrivalOn(),
    previousDepartureOn = this.previousDepartureOn(),
    characteristics = this.characteristics(),
    previousCharacteristics = this.previousCharacteristics(),
  )
}
