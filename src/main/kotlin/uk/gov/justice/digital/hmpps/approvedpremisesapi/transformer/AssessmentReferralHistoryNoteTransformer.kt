package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistorySystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType

@Component
class AssessmentReferralHistoryNoteTransformer {
  fun transformJpaToApi(jpa: AssessmentReferralHistoryNoteEntity): ReferralHistoryNote = when (jpa) {
    is AssessmentReferralHistoryUserNoteEntity -> ReferralHistoryUserNote(
      id = jpa.id,
      createdAt = jpa.createdAt.toInstant(),
      message = jpa.message,
      createdByUserName = jpa.createdByUser.name,
      type = "user",
    )
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

  private fun transformSystemNoteTypeToCategory(type: ReferralHistorySystemNoteType): ReferralHistorySystemNote.Category = when (type) {
    ReferralHistorySystemNoteType.SUBMITTED -> ReferralHistorySystemNote.Category.submitted
    ReferralHistorySystemNoteType.UNALLOCATED -> ReferralHistorySystemNote.Category.unallocated
    ReferralHistorySystemNoteType.IN_REVIEW -> ReferralHistorySystemNote.Category.inReview
    ReferralHistorySystemNoteType.READY_TO_PLACE -> ReferralHistorySystemNote.Category.readyToPlace
    ReferralHistorySystemNoteType.REJECTED -> ReferralHistorySystemNote.Category.rejected
    ReferralHistorySystemNoteType.COMPLETED -> ReferralHistorySystemNote.Category.completed
  }
}
