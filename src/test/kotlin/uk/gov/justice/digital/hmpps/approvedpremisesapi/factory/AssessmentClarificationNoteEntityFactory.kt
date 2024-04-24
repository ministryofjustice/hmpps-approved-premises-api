package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentClarificationNoteEntityFactory : Factory<AssessmentClarificationNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var assessment: Yielded<AssessmentEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var createdBy: Yielded<UserEntity>? = null
  private var query: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var response: Yielded<String?> = { null }
  private var responseReceivedOn: Yielded<LocalDate?> = { null }
  private var hasDomainEvent: Yielded<Boolean> = { false }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAssessment(assessment: AssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy }
  }

  fun withQuery(query: String) = apply {
    this.query = { query }
  }

  fun withResponse(response: String?) = apply {
    this.response = { response }
  }

  fun withResponseReceivedOn(responseReceivedOn: LocalDate) = apply {
    this.responseReceivedOn = { responseReceivedOn }
  }

  fun withHasDomainEvent(hasDomainEvent: Boolean) = apply {
    this.hasDomainEvent = { hasDomainEvent }
  }

  override fun produce(): AssessmentClarificationNoteEntity = AssessmentClarificationNoteEntity(
    id = this.id(),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an assessment"),
    createdAt = this.createdAt(),
    createdByUser = this.createdBy?.invoke() ?: throw RuntimeException("Must provide a createdBy"),
    query = this.query(),
    response = this.response(),
    responseReceivedOn = this.responseReceivedOn(),
    hasDomainEvent = this.hasDomainEvent(),
  )
}
