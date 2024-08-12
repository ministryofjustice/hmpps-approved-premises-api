package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class ConfirmationEntityFactory : Factory<ConfirmationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var dateTime: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(14) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<BookingEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDateTime(dateTime: OffsetDateTime) = apply {
    this.dateTime = { dateTime }
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

  override fun produce(): ConfirmationEntity = ConfirmationEntity(
    id = this.id(),
    notes = this.notes(),
    dateTime = this.dateTime(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided"),
    createdAt = this.createdAt(),
  )
}
