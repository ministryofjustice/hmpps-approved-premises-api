package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository

@Component
class UserAllocator(
  private val userAllocatorRules: List<UserAllocatorRule>,
  private val userRepository: UserRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val userAllocatorRulesInPriorityOrder: List<UserAllocatorRule> by lazy {
    userAllocatorRules.sortedBy { it.priority }
  }

  fun getUserForAssessmentAllocation(assessmentEntity: AssessmentEntity): UserEntity? =
    getUserForAllocation { it.evaluateAssessment(assessmentEntity) }

  fun getUserForPlacementRequestAllocation(placementRequestEntity: PlacementRequestEntity): UserEntity? =
    getUserForAllocation { it.evaluatePlacementRequest(placementRequestEntity) }

  fun getUserForPlacementApplicationAllocation(placementApplicationEntity: PlacementApplicationEntity): UserEntity? =
    getUserForAllocation { it.evaluatePlacementApplication(placementApplicationEntity) }

  private fun getUserForAllocation(evaluate: (UserAllocatorRule) -> UserAllocatorRuleOutcome): UserEntity? {
    userAllocatorRulesInPriorityOrder.forEach { rule ->
      when (val outcome = evaluate(rule)) {
        is UserAllocatorRuleOutcome.AllocateToUser -> {
          when (val user = userRepository.findByDeliusUsername(outcome.userName)) {
            null -> {
              log.warn("Rule '${rule.name}' attempted to allocate a task to user '${outcome.userName}', but they could not be found. This rule has been skipped.")
            }
            else -> return user
          }
        }

        UserAllocatorRuleOutcome.DoNotAllocate -> return null

        UserAllocatorRuleOutcome.Skip -> {
          // Do nothing.
        }
      }
    }

    return null
  }
}
