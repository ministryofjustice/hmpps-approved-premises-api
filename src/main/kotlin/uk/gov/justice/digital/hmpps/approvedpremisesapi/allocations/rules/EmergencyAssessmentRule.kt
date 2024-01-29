package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification

@Component
@ConditionalOnProperty(name = ["user-allocations.rules.emergency-assessments.enabled"])
class EmergencyAssessmentRule(
  @Value("\${user-allocations.rules.emergency-assessments.priority:0}")
  override val priority: Int,
) : UserAllocatorRule {
  override val name: String
    get() = "Emergency assessments"

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (application.isEmergencyApplication) {
      true -> UserAllocatorRuleOutcome.AllocateByQualification(UserQualification.EMERGENCY)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }
}
