package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

interface UserAllocatorRule {
  val name: String

  val priority: Int
    get() = 0

  fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome =
    UserAllocatorRuleOutcome.Skip

  fun evaluatePlacementRequest(placementRequestEntity: PlacementRequestEntity): UserAllocatorRuleOutcome =
    UserAllocatorRuleOutcome.Skip

  fun evaluatePlacementApplication(placementApplicationEntity: PlacementApplicationEntity): UserAllocatorRuleOutcome =
    UserAllocatorRuleOutcome.Skip
}
