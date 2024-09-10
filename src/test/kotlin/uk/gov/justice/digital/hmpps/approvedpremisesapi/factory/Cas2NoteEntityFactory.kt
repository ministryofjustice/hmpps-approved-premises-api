package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2NoteEntityFactory : Factory<Cas2ApplicationNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdByUser: Yielded<Cas2User> = { NomisUserEntityFactory().produce() }
  private var application: Yielded<Cas2ApplicationEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var body: Yielded<String> = { "Note body" }
  private var assessment: Yielded<Cas2AssessmentEntity?> = { null }

  fun withApplication(application: Cas2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessment(assessment: Cas2AssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withCreatedByUser(createdByUser: NomisUserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withBody(body: String) = apply {
    this.body = { body }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): Cas2ApplicationNoteEntity = Cas2ApplicationNoteEntity(
    id = this.id(),
    createdByUser = this.createdByUser(),
    application = this.application?.invoke() ?: error("Must provide an application."),
    createdAt = this.createdAt(),
    body = this.body(),
    assessment = this.assessment(),
  )
}
