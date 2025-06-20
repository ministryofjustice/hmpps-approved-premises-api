package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ExtensionEntityFactory : Factory<Cas3ExtensionEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var previousDepartureDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var newDepartureDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<Cas3BookingEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPreviousDepartureDate(previousDepartureDate: LocalDate) = apply {
    this.previousDepartureDate = { previousDepartureDate }
  }

  fun withNewDepartureDate(newDepartureDate: LocalDate) = apply {
    this.newDepartureDate = { newDepartureDate }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedBooking(booking: Yielded<Cas3BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: Cas3BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withDefaults() = apply {
    withBooking(Cas3BookingEntityFactory().withDefaults().produce())
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3ExtensionEntity = Cas3ExtensionEntity(
    id = this.id(),
    previousDepartureDate = this.previousDepartureDate(),
    newDepartureDate = this.newDepartureDate(),
    notes = this.notes(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided"),
    createdAt = this.createdAt(),
  )
}
