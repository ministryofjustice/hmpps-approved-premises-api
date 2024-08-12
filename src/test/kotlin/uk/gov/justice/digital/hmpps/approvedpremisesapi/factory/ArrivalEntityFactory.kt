package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ArrivalEntityFactory : Factory<ArrivalEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore() }
  private var arrivalDateTime: Yielded<Instant> = { Instant.now().randomDateTimeBefore() }
  private var expectedDepartureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter() }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<BookingEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withArrivalDateTime(arrivalDateTime: Instant) = apply {
    this.arrivalDateTime = { arrivalDateTime }
  }

  fun withExpectedDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedBooking(booking: Yielded<BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): ArrivalEntity = ArrivalEntity(
    id = this.id(),
    arrivalDate = this.arrivalDate(),
    arrivalDateTime = this.arrivalDateTime(),
    expectedDepartureDate = this.expectedDepartureDate(),
    notes = this.notes(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided"),
    createdAt = this.createdAt(),
  )
}
