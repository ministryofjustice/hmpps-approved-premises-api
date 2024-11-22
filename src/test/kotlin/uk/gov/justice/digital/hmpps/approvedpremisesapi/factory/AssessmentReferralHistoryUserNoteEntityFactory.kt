package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentReferralHistoryUserNoteEntityFactory : Factory<AssessmentReferralHistoryUserNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var assessment: Yielded<AssessmentEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var message: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var createdBy: Yielded<UserEntity>? = null

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

  override fun produce(): AssessmentReferralHistoryUserNoteEntity = AssessmentReferralHistoryUserNoteEntity(
    id = this.id(),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an assessment"),
    createdAt = this.createdAt(),
    message = this.message(),
    createdByUser = this.createdBy?.invoke() ?: this.assessment?.invoke()?.application?.createdByUser
      ?: throw RuntimeException("Must provide a createdBy"),
  )
}
