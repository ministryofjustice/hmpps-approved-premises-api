package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

@Component
class AssessmentListener {
  @PrePersist
  fun prePersist(assessment: ApprovedPremisesAssessmentEntity) {
    if (assessment.allocatedToUser == null) {
      (assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT
    } else {
      (assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
    }
  }

  @PreUpdate
  fun preUpdate(assessment: ApprovedPremisesAssessmentEntity) {
    if (assessment.decision == null && assessment.data != null) {
      (assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
    } else if (assessment.decision == AssessmentDecision.ACCEPTED) {
      (assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
    } else {
      (assessment.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.REJECTED
    }
  }
}
