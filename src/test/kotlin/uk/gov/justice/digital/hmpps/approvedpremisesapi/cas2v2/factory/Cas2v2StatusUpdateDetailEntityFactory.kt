package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2StatusUpdateDetailEntityFactory : Factory<Cas2v2StatusUpdateDetailEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var statusDetailId: Yielded<UUID> = { UUID.randomUUID() }
  private var statusUpdate: Yielded<Cas2v2StatusUpdateEntity>? = null
  private var label: Yielded<String> = { "More Detail Required" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }
  fun withStatusDetailId(statusDetailId: UUID) = apply {
    this.statusDetailId = { statusDetailId }
  }

  fun withStatusUpdate(statusUpdate: Cas2v2StatusUpdateEntity) = apply {
    this.statusUpdate = { statusUpdate }
  }

  fun withLabel(label: String) = apply {
    this.label = { label }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): Cas2v2StatusUpdateDetailEntity = Cas2v2StatusUpdateDetailEntity(
    id = this.id(),
    statusDetailId = this.statusDetailId(),
    statusUpdate = this.statusUpdate?.invoke() ?: error("Must provide a status update"),
    label = this.label(),
    createdAt = this.createdAt(),
  )
}
