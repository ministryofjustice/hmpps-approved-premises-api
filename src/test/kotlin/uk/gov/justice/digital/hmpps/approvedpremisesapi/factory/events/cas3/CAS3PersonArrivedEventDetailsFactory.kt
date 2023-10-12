package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CAS3PersonArrivedEventDetailsFactory : Factory<CAS3PersonArrivedEventDetails> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { PremisesFactory().produce() }
  private var arrivedAt: Yielded<Instant> = { Instant.now() }
  private var expectedDepartureOn: Yielded<LocalDate> = { LocalDate.now().randomDateAfter() }
  private var notes: Yielded<String> = { randomStringLowerCase(20) }
  private var applicationId: Yielded<UUID?> = { null }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withPremises(configuration: PremisesFactory.() -> Unit) = apply {
    this.premises = { PremisesFactory().apply(configuration).produce() }
  }

  fun withArrivedAt(arrivedAt: Instant) = apply {
    this.arrivedAt = { arrivedAt }
  }

  fun withExpectedDepartureOn(expectedDepartureOn: LocalDate) = apply {
    this.expectedDepartureOn = { expectedDepartureOn }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withApplicationId(applicationId: UUID?) = apply {
    this.applicationId = { applicationId }
  }

  override fun produce() = CAS3PersonArrivedEventDetails(
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    premises = this.premises(),
    arrivedAt = this.arrivedAt(),
    expectedDepartureOn = this.expectedDepartureOn(),
    notes = this.notes(),
    applicationId = this.applicationId(),
  )
}
