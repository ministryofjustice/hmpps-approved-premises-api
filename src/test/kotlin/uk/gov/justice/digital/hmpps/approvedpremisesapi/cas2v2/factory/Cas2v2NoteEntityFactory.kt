package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2NoteEntityFactory : Factory<Cas2v2ApplicationNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdByUser: Yielded<Cas2v2UserEntity> = { Cas2v2UserEntityFactory().produce() }
  private var application: Yielded<Cas2v2ApplicationEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var body: Yielded<String> = { "Note body" }
  private var assessment: Yielded<Cas2v2AssessmentEntity?> = { null }

  fun withApplication(application: Cas2v2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessment(assessment: Cas2v2AssessmentEntity?) = apply {
    this.assessment = { assessment }
  }

  fun withCreatedByUser(createdByUser: Cas2v2UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withBody(body: String) = apply {
    this.body = { body }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): Cas2v2ApplicationNoteEntity = Cas2v2ApplicationNoteEntity(
    id = this.id(),
    createdByUser = this.createdByUser(),
    application = this.application?.invoke() ?: error("Must provide an application."),
    createdAt = this.createdAt(),
    body = this.body(),
    assessment = this.assessment(),
  )
}
