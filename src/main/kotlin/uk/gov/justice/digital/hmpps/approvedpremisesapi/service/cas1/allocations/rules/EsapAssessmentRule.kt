package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome

@SuppressWarnings("MagicNumber")
@Component
class EsapAssessmentRule(
  @Value("\${user-allocations.rules.esap-assessments.allocate-to-user}")
  private val allocateToUser: String,
) : UserAllocatorRule {
  override val name: String
    get() = "ESAP assessments"

  override val priority: Int
    get() = 3

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (application.isEsapApplication) {
      true -> UserAllocatorRuleOutcome.AllocateToUser(allocateToUser)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }
}
