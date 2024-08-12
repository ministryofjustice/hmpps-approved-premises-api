package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class LostBedCancellationEntityFactory : Factory<LostBedCancellationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(14) }
  private var notes: Yielded<String>? = null
  private var lostBed: Yielded<LostBedsEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedLostBed(lostBed: Yielded<LostBedsEntity>) = apply {
    this.lostBed = lostBed
  }

  fun withLostBed(lostBed: LostBedsEntity) = apply {
    this.lostBed = { lostBed }
  }

  override fun produce() = LostBedCancellationEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    notes = this.notes?.invoke(),
    lostBed = this.lostBed?.invoke() ?: throw RuntimeException("Lost Bed must be provided"),
  )
}
