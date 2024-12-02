package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome

@SuppressWarnings("MagicNumber")
@Component
class EmergencyAndShortNoticeAssessmentRule : UserAllocatorRule {
  override val name: String
    get() = "Emergency assessments"

  override val priority: Int
    get() = 2

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (
      application.noticeType == Cas1ApplicationTimelinessCategory.EMERGENCY ||
        application.noticeType == Cas1ApplicationTimelinessCategory.SHORT_NOTICE
    ) {
      true -> allocateByCruManagementArea(application.cruManagementArea!!)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }

  private fun allocateByCruManagementArea(cruManagementArea: Cas1CruManagementAreaEntity) =
    when (val userName = cruManagementArea.assessmentAutoAllocationUsername) {
      null -> UserAllocatorRuleOutcome.Skip
      else -> UserAllocatorRuleOutcome.AllocateToUser(userName)
    }
}
