package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Component
class AssessmentClarificationNoteListener {

  fun prePersist(clarificationNote: AssessmentClarificationNoteEntity) {
    if (clarificationNote.response == null && clarificationNote.assessment.application is ApprovedPremisesApplicationEntity) {
      (clarificationNote.assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION
    }
  }

  fun preUpdate(clarificationNote: AssessmentClarificationNoteEntity) {
    if (clarificationNote.response != null && clarificationNote.assessment.application is ApprovedPremisesApplicationEntity) {
      (clarificationNote.assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
    }
  }
}
