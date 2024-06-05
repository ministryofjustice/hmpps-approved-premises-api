package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Component
class AssessmentListener {
  fun prePersist(assessment: ApprovedPremisesAssessmentEntity) {
    if (assessment.allocatedToUser == null) {
      (assessment.application as ApprovedPremisesApplicationEntity).status =
        ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT
    } else {
      (assessment.application as ApprovedPremisesApplicationEntity).status =
        ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
    }
  }

  fun preUpdate(assessment: ApprovedPremisesAssessmentEntity) {
    val application = assessment.application as ApprovedPremisesApplicationEntity

    if (application.status == ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION && assessment.decision == null) {
      return
    } else if (assessment.isWithdrawn) {
      return
    } else if (assessment.decision == null && assessment.data != null) {
      application.status = ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
    } else if (assessment.decision == AssessmentDecision.ACCEPTED) {
      application.status = applicationGetStatusFromArrivalDate(application)
    } else if (assessment.decision == AssessmentDecision.REJECTED) {
      application.status = ApprovedPremisesApplicationStatus.REJECTED
    }
  }

  fun applicationGetStatusFromArrivalDate(
    application: ApprovedPremisesApplicationEntity,
  ): ApprovedPremisesApplicationStatus {
    val releaseDate = application.arrivalDate
    if (releaseDate == null) {
      return ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST
    }
    return ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
  }
}
