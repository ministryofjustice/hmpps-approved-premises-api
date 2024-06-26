package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OutOfServiceBedEntityFactory : Factory<Cas1OutOfServiceBedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var bed: Yielded<BedEntity> = { BedEntityFactory().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withBed(bed: BedEntity) = apply {
    this.bed = { bed }
  }

  fun withBed(configuration: BedEntityFactory.() -> Unit) = apply {
    this.bed = { BedEntityFactory().apply(configuration).produce() }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): Cas1OutOfServiceBedEntity {
    val bed = this.bed()

    return Cas1OutOfServiceBedEntity(
      id = this.id(),
      premises = bed.room.premises as ApprovedPremisesEntity,
      bed = bed,
      createdAt = this.createdAt(),
      cancellation = null,
      revisionHistory = mutableListOf(),
    )
  }
}
