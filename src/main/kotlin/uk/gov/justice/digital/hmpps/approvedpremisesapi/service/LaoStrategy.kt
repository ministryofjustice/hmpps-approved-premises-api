package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.ExclusionHandling
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.RestrictionHandling

/**
 * A Limited Access Offender (LAO) is one for which access is limited to
 * specific delius users.
 *
 * An Offender is an LAO if they are configured with either a restriction
 * or an exclusion in Delius. For restrictions, the calling user is allowed
 * to view the offender if they are in an 'allow list' configured in Delius.
 * For exclusions, the calling user can view the offender unless they are
 * in a 'deny list' configured in Delius.
 *
 * When retrieving an offender boolean fields are provided to indicate if the
 * offender is an LAO ([CaseSummary.currentExclusion] and [CaseSummary.currentRestriction]).
 * A separate 'check access' API call can then be made to determine if the calling delius
 * user can access the offender's information.
 *
 * The ap-and-delius API does not enforce these permissions, it is the responsibility
 * of the calling client (i.e. CAS) to check and apply them as applicable.
 *
 * The [LaoStrategy] is used to determine how an LAO should be handled, with one or
 * more strategies defined at the bottom of this file for each CAS
 *
 * The only exception is CAS2 HDC, which implements LAO logic in its own code. HDC
 * uses the same strategy employed by CAS2 for NOMIS users, although ideally it would
 * use the same strategy as CAS2
 */
data class LaoStrategy(
  val restrictionHandling: RestrictionHandling,
  val exclusionHandling: ExclusionHandling,
  val deliusUsername: String? = null,
) {

  init {
    if (restrictionHandling == RestrictionHandling.ALWAYS_CHECK || exclusionHandling == ExclusionHandling.ALWAYS_CHECK) {
      requireNotNull(deliusUsername) { "Delius username is required when applying LAO checks" }
    }
  }

  enum class RestrictionHandling {
    /**
     * If a restriction exists, check whether it applies to the calling Delius user
     */
    ALWAYS_CHECK,

    /**
     * If a restriction exists, block access for all users without checking whether it
     * applies to the calling user (e.g. for users that don't have a Delius account)
     */
    BLOCK_REGARDLESS_OF_USER,

    /**
     * Ignore restrictions
     */
    IGNORE,
  }
  enum class ExclusionHandling {
    ALWAYS_CHECK,
    IGNORE,
  }

  companion object {

    /**
     * Even if the calling user has exclusions or restrictions for a given offender,
     * return [PersonSummaryInfoResult.Success.Full] or [PersonInfoResult.Success.Full] regardless.
     *
     * This strategy should be used with care, typically when the calling user
     * has a specific qualification that indicates they can always view limited
     * access offender information
     */
    val Ignore = LaoStrategy(
      restrictionHandling = RestrictionHandling.IGNORE,
      exclusionHandling = ExclusionHandling.IGNORE,
      deliusUsername = null,
    )

    /**
     * If the offender is LAO, retrieve the access information for the calling delius user.
     * If the user has limited access to this offender (either restricted or excluded), return
     * [PersonInfoResult.Success.Restricted] or [PersonSummaryInfoResult.Success.Restricted]
     */
    fun checkUserAccess(deliusUserName: String) = LaoStrategy(
      restrictionHandling = RestrictionHandling.ALWAYS_CHECK,
      exclusionHandling = ExclusionHandling.ALWAYS_CHECK,
      deliusUsername = deliusUserName,
    )
  }

  /**
   * return true if the strategy means callers can view any offenders
   * without any LAO checks required
   */
  fun isNeverRestricted() = restrictionHandling == RestrictionHandling.IGNORE && exclusionHandling == ExclusionHandling.IGNORE

  /**
   * The CaseSummary indicates if any restrictions/exclusions exist on the CRN,
   * but doesn't indicate if they apply to this specific user
   *
   * Depending on the LaoStrategy, the CaseSummary may be sufficient to determine access
   */
  fun canView(caseSummary: CaseSummary): CaseSummaryViewResult {
    val checkCaseAccessToDetermineExclusion = caseSummary.currentExclusion &&
      when (exclusionHandling) {
        ExclusionHandling.ALWAYS_CHECK -> true
        ExclusionHandling.IGNORE -> false
      }

    val checkCaseAccessToDetermineRestriction = caseSummary.currentRestriction &&
      when (restrictionHandling) {
        RestrictionHandling.ALWAYS_CHECK -> true
        RestrictionHandling.BLOCK_REGARDLESS_OF_USER -> false
        RestrictionHandling.IGNORE -> false
      }

    return if (caseSummary.currentRestriction && restrictionHandling == RestrictionHandling.BLOCK_REGARDLESS_OF_USER) {
      CaseSummaryViewResult.CANNOT_VIEW
    } else if (checkCaseAccessToDetermineExclusion || checkCaseAccessToDetermineRestriction) {
      CaseSummaryViewResult.CHECK_CASE_ACCESS
    } else {
      CaseSummaryViewResult.CAN_VIEW
    }
  }

  /**
   * CaseAccess indicates if there are any restrictions/exclusions that apply to a specific user.
   */
  fun canView(caseAccess: CaseAccess): Boolean {
    val exclusionDoesntApply = when (exclusionHandling) {
      ExclusionHandling.ALWAYS_CHECK -> !caseAccess.userExcluded
      ExclusionHandling.IGNORE -> true
    }

    val restrictionDoesntApply = when (restrictionHandling) {
      RestrictionHandling.ALWAYS_CHECK -> !caseAccess.userRestricted
      RestrictionHandling.BLOCK_REGARDLESS_OF_USER -> false
      RestrictionHandling.IGNORE -> true
    }

    return exclusionDoesntApply && restrictionDoesntApply
  }

  enum class CaseSummaryViewResult {
    CAN_VIEW,
    CANNOT_VIEW,
    CHECK_CASE_ACCESS,
  }
}

/**
 * This strategy is _not_ applied when creating an application. In that case, use [cas1CreateApplicationLaoStrategy]
 */
fun UserEntity.cas1LaoStrategy() = if (this.hasQualification(UserQualification.LAO)) {
  LaoStrategy.Ignore
} else {
  LaoStrategy.checkUserAccess(this.deliusUsername)
}

fun UserEntity.cas1CreateApplicationLaoStrategy() = LaoStrategy.checkUserAccess(this.deliusUsername)

fun Cas2UserEntity.cas2LaoStrategy(): LaoStrategy = when (userType) {
  Cas2UserType.DELIUS -> LaoStrategy.checkUserAccess(username)
  Cas2UserType.NOMIS -> LaoStrategy(
    restrictionHandling = RestrictionHandling.BLOCK_REGARDLESS_OF_USER,
    exclusionHandling = ExclusionHandling.IGNORE,
  )
  Cas2UserType.EXTERNAL -> LaoStrategy.Ignore
}

fun UserEntity.cas3LaoStrategy() = LaoStrategy.checkUserAccess(this.deliusUsername)
