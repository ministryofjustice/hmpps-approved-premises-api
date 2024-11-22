package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentReferralHistorySystemNoteEntityFactory : Factory<AssessmentReferralHistorySystemNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var assessment: Yielded<AssessmentEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var message: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var createdBy: Yielded<UserEntity>? = null
  private var type: Yielded<ReferralHistorySystemNoteType> = { randomOf(ReferralHistorySystemNoteType.entries) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAssessment(assessment: AssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withMessage(message: String) = apply {
    this.message = { message }
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy }
  }

  fun withType(type: ReferralHistorySystemNoteType) = apply {
    this.type = { type }
  }

  override fun produce(): AssessmentReferralHistorySystemNoteEntity = AssessmentReferralHistorySystemNoteEntity(
    id = this.id(),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an assessment"),
    createdAt = this.createdAt(),
    message = this.message(),
    createdByUser = this.createdBy?.invoke() ?: this.assessment?.invoke()!!.application.createdByUser,
    type = this.type(),
  )
}
