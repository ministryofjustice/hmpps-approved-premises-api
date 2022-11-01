package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentEntityFactory : Factory<AssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var application: Yielded<ApplicationEntity>? = null
  private var assessmentSchema: Yielded<JsonSchemaEntity> = {
    JsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
      type = JsonSchemaType.ASSESSMENT
    )
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var allocatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var decision: Yielded<AssessmentDecision> = { AssessmentDecision.ACCEPTED }
  private var allocatedToUser: Yielded<UserEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withData(data: String) = apply {
    this.data = { data }
  }

  fun withDocument(document: String) = apply {
    this.document = { document }
  }

  fun withApplication(application: ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessmentSchema(assessmentSchema: JsonSchemaEntity) = apply {
    this.assessmentSchema = { assessmentSchema }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withAllocatedAt(allocatedAt: OffsetDateTime) = apply {
    this.allocatedAt = { allocatedAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withDecision(decision: AssessmentDecision) = apply {
    this.decision = { decision }
  }

  fun withAllocatedToUser(allocatedToUser: UserEntity) = apply {
    this.allocatedToUser = { allocatedToUser }
  }

  override fun produce(): AssessmentEntity = AssessmentEntity(
    id = this.id(),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.assessmentSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    decision = this.decision(),
    schemaUpToDate = false,
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an application"),
    allocatedToUser = this.allocatedToUser?.invoke() ?: throw RuntimeException("Must provide an allocatedToUser"),
    allocatedAt = this.allocatedAt()
  )
}
