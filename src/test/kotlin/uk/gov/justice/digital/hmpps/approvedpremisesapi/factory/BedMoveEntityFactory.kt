package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("TooGenericExceptionThrown")
class BedMoveEntityFactory : Factory<BedMoveEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var booking: Yielded<BookingEntity>? = null
  private var previousBed: Yielded<BedEntity>? = null
  private var newBed: Yielded<BedEntity>? = null
  private var notes: Yielded<String> = { "TEST NOTES" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withPreviousBed(previousBed: BedEntity) = apply {
    this.previousBed = { previousBed }
  }

  fun withNewBed(newBed: BedEntity) = apply {
    this.newBed = { newBed }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): BedMoveEntity = BedMoveEntity(
    id = this.id(),

    booking = this.booking?.invoke() ?: throw RuntimeException("Must provide a booking"),
    previousBed = this.previousBed?.invoke() ?: throw RuntimeException("Must provide previous bed"),
    newBed = this.newBed?.invoke() ?: throw RuntimeException("Must provide new bed"),
    notes = this.notes(),
    createdAt = this.createdAt(),
  )
}
