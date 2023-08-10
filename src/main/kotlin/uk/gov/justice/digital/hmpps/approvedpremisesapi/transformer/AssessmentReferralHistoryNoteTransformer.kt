package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity

@Component
class AssessmentReferralHistoryNoteTransformer {
  fun transformJpaToApi(jpa: AssessmentReferralHistoryNoteEntity): ReferralHistoryNote = when (jpa) {
    is AssessmentReferralHistoryUserNoteEntity -> ReferralHistoryUserNote(
      id = jpa.id,
      createdAt = jpa.createdAt.toInstant(),
      message = jpa.message,
      createdByStaffMemberId = jpa.createdByUser.id,
    )
    else -> throw RuntimeException("Unsupported ReferralHistoryNote type: ${jpa::class.qualifiedName}")
  }
}
