package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryDomainEventNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNoteMessageDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistorySystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

@Component
class AssessmentReferralHistoryNoteTransformer(
  private val objectMapper: ObjectMapper,
) {

  fun transformJpaToApi(jpa: AssessmentReferralHistoryNoteEntity): ReferralHistoryNote = when (jpa) {
    is AssessmentReferralHistoryUserNoteEntity -> {
      transformJpaToReferralHistoryUserNote(jpa)
    }
    is AssessmentReferralHistorySystemNoteEntity -> ReferralHistorySystemNote(
      id = jpa.id,
      createdAt = jpa.createdAt.toInstant(),
      message = jpa.message,
      createdByUserName = jpa.createdByUser.name,
      type = "system",
      category = transformSystemNoteTypeToCategory(jpa.type),
    )
    else -> throw RuntimeException("Unsupported ReferralHistoryNote type: ${jpa::class.qualifiedName}")
  }

  fun transformJpaToApi(jpa: AssessmentReferralHistoryNoteEntity, assessmentEntity: TemporaryAccommodationAssessmentEntity, includeUserNotes: Boolean): ReferralHistoryNote = when (jpa) {
    is AssessmentReferralHistoryUserNoteEntity -> {
      transformJpaToReferralHistoryUserNote(jpa)
    }
    is AssessmentReferralHistorySystemNoteEntity -> ReferralHistorySystemNote(
      id = jpa.id,
      createdAt = jpa.createdAt.toInstant(),
      message = jpa.message,
      messageDetails = getMessageDetails(jpa.type, assessmentEntity, includeUserNotes),
      createdByUserName = jpa.createdByUser.name,
      type = "system",
      category = transformSystemNoteTypeToCategory(jpa.type),
    )
    else -> throw RuntimeException("Unsupported ReferralHistoryNote type: ${jpa::class.qualifiedName}")
  }

  fun transformToReferralHistoryDomainEventNote(domainEventEntity: DomainEventEntity, user: UserEntity) =
    ReferralHistoryDomainEventNote(
      id = domainEventEntity.id,
      createdAt = domainEventEntity.createdAt.toInstant(),
      messageDetails = ReferralHistoryNoteMessageDetails(domainEvent = objectMapper.readTree(domainEventEntity.data)),
      createdByUserName = user.name,
      type = "domainEvent",
    )

  private fun transformJpaToReferralHistoryUserNote(jpa: AssessmentReferralHistoryNoteEntity): ReferralHistoryUserNote {
    return ReferralHistoryUserNote(
      id = jpa.id,
      createdAt = jpa.createdAt.toInstant(),
      message = jpa.message,
      createdByUserName = jpa.createdByUser.name,
      type = "user",
    )
  }

  private fun getMessageDetails(
    systemNoteType: ReferralHistorySystemNoteType,
    assessmentEntity: TemporaryAccommodationAssessmentEntity,
    includeUserNotes: Boolean,
  ): ReferralHistoryNoteMessageDetails? {
    if (systemNoteType == ReferralHistorySystemNoteType.REJECTED && assessmentEntity.referralRejectionReason != null) {
      return ReferralHistoryNoteMessageDetails(
        assessmentEntity.referralRejectionReason?.name,
        if (includeUserNotes) assessmentEntity.referralRejectionReasonDetail else null,
        assessmentEntity.isWithdrawn,
      )
    }
    return null
  }

  private fun transformSystemNoteTypeToCategory(type: ReferralHistorySystemNoteType): ReferralHistorySystemNote.Category = when (type) {
    ReferralHistorySystemNoteType.SUBMITTED -> ReferralHistorySystemNote.Category.submitted
    ReferralHistorySystemNoteType.UNALLOCATED -> ReferralHistorySystemNote.Category.unallocated
    ReferralHistorySystemNoteType.IN_REVIEW -> ReferralHistorySystemNote.Category.inReview
    ReferralHistorySystemNoteType.READY_TO_PLACE -> ReferralHistorySystemNote.Category.readyToPlace
    ReferralHistorySystemNoteType.REJECTED -> ReferralHistorySystemNote.Category.rejected
    ReferralHistorySystemNoteType.COMPLETED -> ReferralHistorySystemNote.Category.completed
  }
}
