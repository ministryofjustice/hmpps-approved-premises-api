package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations

sealed interface UserAllocatorRuleOutcome {
  /**
   * Continue processing the next rule.
   */
  data object Skip : UserAllocatorRuleOutcome

  /**
   * Do not allocate a user to the task.
   */
  data object DoNotAllocate : UserAllocatorRuleOutcome

  /**
   * Allocate a specific user to the task.
   */
  class AllocateToUser(val userName: String) : UserAllocatorRuleOutcome
}
