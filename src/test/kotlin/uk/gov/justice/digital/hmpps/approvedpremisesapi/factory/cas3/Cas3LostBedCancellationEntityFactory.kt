package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas3LostBedCancellationEntityFactory : Factory<Cas3LostBedCancellationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(14) }
  private var notes: Yielded<String>? = null
  private var lostBed: Yielded<Cas3LostBedsEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedLostBed(lostBed: Yielded<Cas3LostBedsEntity>) = apply {
    this.lostBed = lostBed
  }

  fun withLostBed(lostBed: Cas3LostBedsEntity) = apply {
    this.lostBed = { lostBed }
  }

  @Suppress("TooGenericExceptionThrown")
  override fun produce() = Cas3LostBedCancellationEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    notes = this.notes?.invoke(),
    lostBed = this.lostBed?.invoke() ?: throw RuntimeException("Lost Bed must be provided"),
  )
}
