package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationSchemaEntityFactory : Factory<ApplicationSchemaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var addedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var schema: Yielded<String> = { "{}" }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAddedAt(addedAt: OffsetDateTime) = apply {
    this.addedAt = { addedAt }
  }

  fun withSchema(schema: String) = apply {
    this.schema = { schema }
  }

  override fun produce(): ApplicationSchemaEntity = ApplicationSchemaEntity(
    id = this.id(),
    addedAt = this.addedAt(),
    schema = this.schema()
  )
}
