package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationEntityFactory : Factory<PlacementApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdByUser: Yielded<UserEntity>? = null
  private var application: Yielded<ApplicationEntity>? = null
  private var schemaVersion: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesPlacementApplicationJsonSchemaEntityFactory().produce()
  }
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedByUser(createdByUser: UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withSchemaVersion(schemaVersion: JsonSchemaEntity) = apply {
    this.schemaVersion = { schemaVersion }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withApplication(applicationEntity: ApplicationEntity) = apply {
    this.application = { applicationEntity }
  }

  override fun produce(): PlacementApplicationEntity = PlacementApplicationEntity(
    id = this.id(),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an application"),
    schemaVersion = this.schemaVersion(),
    data = this.data(),
    document = this.document(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    schemaUpToDate = false,
    allocatedToUser = null,
    allocatedAt = null,
    reallocatedAt = null,
    decision = null,
  )
}
