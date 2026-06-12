package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome

@SuppressWarnings("MagicNumber")
@Component
class EsapAssessmentRule(
  @Value("\${cas1.allocations.esap-assessment.default-username}")
  private val defaultUsername: String,
  @Value("\${cas1.allocations.esap-assessment.arrival-within-28-days-username}")
  private val arrivalWithin28DaysUsername: String,
) : UserAllocatorRule {
  override val name: String
    get() = "ESAP assessments"

  override val priority: Int
    get() = 3

  override fun evaluateAssessment(assessmentEntity: ApprovedPremisesAssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.cas1Application()
    if (application.submittedAt == null || !application.isEsapApplication) return UserAllocatorRuleOutcome.Skip

    return if (application.hasArrivalWithin28Days) {
      UserAllocatorRuleOutcome.AllocateToUser(arrivalWithin28DaysUsername)
    } else {
      UserAllocatorRuleOutcome.AllocateToUser(defaultUsername)
    }
  }
}
