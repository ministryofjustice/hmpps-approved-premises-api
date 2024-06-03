package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OutOfServiceBedCancellationEntityFactory : Factory<Cas1OutOfServiceBedCancellationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var notes: Yielded<String?> = { null }
  private var outOfServiceBed: Yielded<Cas1OutOfServiceBedEntity> = { Cas1OutOfServiceBedEntityFactory().produce() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withOutOfServiceBed(outOfServiceBed: Cas1OutOfServiceBedEntity) = apply {
    this.outOfServiceBed = { outOfServiceBed }
  }

  fun withOutOfServiceBed(configuration: Cas1OutOfServiceBedEntityFactory.() -> Unit) = apply {
    this.outOfServiceBed = { Cas1OutOfServiceBedEntityFactory().apply(configuration).produce() }
  }

  override fun produce() = Cas1OutOfServiceBedCancellationEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    notes = this.notes(),
    outOfServiceBed = this.outOfServiceBed(),
  )
}
