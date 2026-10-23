package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.ExclusionHandling
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.RestrictionHandling

// TODO: these need unit testing
// TODO: then update offender service check access code to use this too (needs some refactoring)

/**
 * The CaseSummary indicates if any restrictions/exclusions exist on the CRN,
 * but doesn't indicate if they apply to this specific user
 *
 * Depending on the LaoStrategy, this may be sufficient to determine access
 */
fun CaseSummary.canView(strategy: LaoStrategy): CaseSummaryViewResult {
  val exclusionDoesntApply = if (!this.currentExclusion) {
    true
  } else {
    when (strategy.exclusionHandling) {
      ExclusionHandling.ALWAYS_CHECK -> null
      ExclusionHandling.IGNORE -> true
    }
  }

  val restrictionDoesntApply = if (!this.currentRestriction) {
    true
  } else {
    when (strategy.restrictionHandling) {
      RestrictionHandling.ALWAYS_CHECK -> null
      RestrictionHandling.IGNORE -> true
    }
  }

  return if (exclusionDoesntApply == true && restrictionDoesntApply == true) {
    CaseSummaryViewResult.CAN_VIEW
  } else {
    CaseSummaryViewResult.UNKNOWN
  }
}

enum class CaseSummaryViewResult {
  CAN_VIEW,

  /**
   * In this case CaseAccess needs checking
   */
  UNKNOWN,
}

/**
 * CaseAccess indicates if there are any restrictions/exclusions that apply to a specific user.
 */
fun CaseAccess.cannotView(strategy: LaoStrategy): Boolean {
  val excluded = when (strategy.exclusionHandling) {
    ExclusionHandling.ALWAYS_CHECK -> this.userExcluded
    ExclusionHandling.IGNORE -> false
  }

  val restricted = when (strategy.restrictionHandling) {
    RestrictionHandling.ALWAYS_CHECK -> this.userRestricted
    RestrictionHandling.IGNORE -> false
  }

  return excluded || restricted
}
