package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome

@SuppressWarnings("MagicNumber")
@Component
class AppealedAssessmentRule(
  private val appealRepository: AppealRepository,
) : UserAllocatorRule {
  override val name: String
    get() = "Appealed assessments"

  override val priority: Int
    get() = 0

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (assessmentEntity !is ApprovedPremisesAssessmentEntity) return UserAllocatorRuleOutcome.Skip
    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (assessmentEntity.createdFromAppeal) {
      true -> allocateToArbitrator(application)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }

  private fun allocateToArbitrator(application: ApplicationEntity): UserAllocatorRuleOutcome {
    val appeal = appealRepository.findByApplication(application).maxByOrNull { it.createdAt }
    return when (appeal) {
      null -> UserAllocatorRuleOutcome.Skip
      else -> UserAllocatorRuleOutcome.AllocateToUser(appeal.createdBy.deliusUsername)
    }
  }
}
