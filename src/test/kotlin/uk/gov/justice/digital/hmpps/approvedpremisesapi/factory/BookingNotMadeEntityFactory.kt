package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class BookingNotMadeEntityFactory : Factory<BookingNotMadeEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(5) }
  private var placementRequest: Yielded<PlacementRequestEntity>? = null
  private var notes: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  override fun produce(): BookingNotMadeEntity = BookingNotMadeEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    placementRequest = this.placementRequest?.invoke() ?: throw RuntimeException("Must provide a Placement Request"),
    notes = this.notes(),
  )
}
