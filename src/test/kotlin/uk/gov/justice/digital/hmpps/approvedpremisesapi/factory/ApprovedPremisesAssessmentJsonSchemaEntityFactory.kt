package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesAssessmentJsonSchemaEntityFactory : Factory<ApprovedPremisesAssessmentJsonSchemaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var addedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var schema: Yielded<String> = { "{}" }

  fun withDefaults() = apply {
    withAddedAt(OffsetDateTime.now())
    withPermissiveSchema()
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAddedAt(addedAt: OffsetDateTime) = apply {
    this.addedAt = { addedAt }
  }

  fun withSchema(schema: String) = apply {
    this.schema = { schema }
  }

  fun withPermissiveSchema() = apply {
    withSchema(
      """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """,
    )
  }

  override fun produce(): ApprovedPremisesAssessmentJsonSchemaEntity = ApprovedPremisesAssessmentJsonSchemaEntity(
    id = this.id(),
    addedAt = this.addedAt(),
    schema = this.schema(),
  )
}
