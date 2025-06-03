package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

fun assertAssessmentHasSystemNote(assessment: AssessmentEntity, createdByUser: UserEntity, type: ReferralHistorySystemNoteType) {
  assertThat(assessment.referralHistoryNotes)
    .hasSize(1)
    .allMatch {
      it is AssessmentReferralHistorySystemNoteEntity &&
        it.createdByUser == createdByUser &&
        it.assessment == assessment &&
        it.type == type
    }
}
