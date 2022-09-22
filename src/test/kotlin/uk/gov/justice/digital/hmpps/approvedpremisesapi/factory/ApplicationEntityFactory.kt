package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationEntityFactory : Factory<ApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdByProbationOfficer: Yielded<ProbationOfficerEntity>? = null
  private var data: Yielded<String?> = { "{}" }
  private var applicationSchema: Yielded<ApplicationSchemaEntity> = {
    ApplicationSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}"
    )
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withCreatedByProbationOfficer(createdByProbationOfficer: ProbationOfficerEntity) = apply {
    this.createdByProbationOfficer = { createdByProbationOfficer }
  }

  fun withYieldedCreatedByProbationOfficer(createdByProbationOfficer: Yielded<ProbationOfficerEntity>) = apply {
    this.createdByProbationOfficer = createdByProbationOfficer
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withApplicationSchema(applicationSchema: ApplicationSchemaEntity) = apply {
    this.applicationSchema = { applicationSchema }
  }

  fun withYieldedApplicationSchema(applicationSchema: Yielded<ApplicationSchemaEntity>) = apply {
    this.applicationSchema = applicationSchema
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  override fun produce(): ApplicationEntity = ApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    createdByProbationOfficer = this.createdByProbationOfficer?.invoke() ?: throw RuntimeException("Must provide a createdByProbationOfficer"),
    data = this.data(),
    schemaVersion = this.applicationSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    schemaUpToDate = false
  )
}
