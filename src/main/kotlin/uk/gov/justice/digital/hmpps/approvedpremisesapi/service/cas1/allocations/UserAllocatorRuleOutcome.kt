package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

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
  data class AllocateToUser(val userName: String) : UserAllocatorRuleOutcome

  /**
   * Allocate to any suitable user that possesses the given qualification.
   */
  data class AllocateByQualification(val qualification: UserQualification) : UserAllocatorRuleOutcome

  /**
   * Allocate to any suitable user that possesses the given role.
   */
  data class AllocateByRole(val role: UserRole) : UserAllocatorRuleOutcome
}
