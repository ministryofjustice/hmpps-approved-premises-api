package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity

interface UserAllocatorRule {
  val name: String

  /**
   * The lower the number, the higher the priority
   */
  val priority: Int

  fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome = UserAllocatorRuleOutcome.Skip

  fun evaluatePlacementApplication(placementApplicationEntity: PlacementApplicationEntity): UserAllocatorRuleOutcome = UserAllocatorRuleOutcome.Skip
}
