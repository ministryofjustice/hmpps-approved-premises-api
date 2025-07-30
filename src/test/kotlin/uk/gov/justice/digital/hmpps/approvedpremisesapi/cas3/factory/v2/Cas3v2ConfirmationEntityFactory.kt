package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2ConfirmationEntityFactory : Factory<Cas3v2ConfirmationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var dateTime: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(14) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<Cas3BookingEntity>? = null
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

  fun withBooking(booking: Cas3BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  @Suppress("TooGenericExceptionThrown")
  override fun produce() = Cas3v2ConfirmationEntity(
    id = this.id(),
    notes = this.notes(),
    dateTime = this.dateTime(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided"),
    createdAt = this.createdAt(),
  )
}
