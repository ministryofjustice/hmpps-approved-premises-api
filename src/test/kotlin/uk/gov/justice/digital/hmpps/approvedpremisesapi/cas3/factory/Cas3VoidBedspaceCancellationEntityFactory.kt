package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas3VoidBedspaceCancellationEntityFactory : Factory<Cas3VoidBedspaceCancellationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(14) }
  private var notes: Yielded<String>? = null
  private var voidBedspace: Yielded<Cas3VoidBedspaceEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedVoidBedspace(voidBedspace: Yielded<Cas3VoidBedspaceEntity>) = apply {
    this.voidBedspace = voidBedspace
  }

  fun withVoidBedspace(voidBedspace: Cas3VoidBedspaceEntity) = apply {
    this.voidBedspace = { voidBedspace }
  }

  @Suppress("TooGenericExceptionThrown")
  override fun produce() = Cas3VoidBedspaceCancellationEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    notes = this.notes?.invoke(),
    voidBedspace = this.voidBedspace?.invoke() ?: throw RuntimeException("Lost Bed must be provided"),
  )
}
