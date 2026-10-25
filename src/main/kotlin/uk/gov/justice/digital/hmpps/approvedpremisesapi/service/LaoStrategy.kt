package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult

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
 * The [LaoStrategy] is used to determine how an LAO should be handled.
 *
 * We want to update all code to explicitly state an [LaoStrategy] when retrieving offender
 * information to make the behaviour clear. Until that has been done, the following sections
 * summarises how each CAS deals with LAOS:
 *
 * # CAS1
 *
 * See [cas1LaoStrategy()] below
 *
 * # CAS2
 *
 * If a restriction exists on the offender (regardless of whether it applies to the user), return [PersonInfoResult.Success.Restricted]
 * If an exclusion exists on the offender, return the full offender info
 *
 * This logic has been applied because CAS2 is used by NOMIS and External Users, so we can't always check restrictions.
 *
 * Implemented in [Cas2HdcOffenderService]
 *
 * # CAS2 Bail
 *
 * If a restriction exists on the offender (regardless of whether it applies to the user), return [PersonInfoResult.Success.Restricted]
 * If an exclusion exists on the offender, return the full offender info
 *
 * This logic has been applied because CAS2 is used by NOMIS, External and Delius users, so we can't always check restrictions.
 *
 * We _could_ tighten this up for Delius users
 *
 * Implemented in [Cas2OffenderService]
 *
 * # CAS3
 *
 * See [cas3LaoStrategy()] below
 */
data class LaoStrategy(
  val restrictionHandling: RestrictionHandling,
  val exclusionHandling: ExclusionHandling,
  val deliusUsername: String?,
) {

  init {
    if (restrictionHandling == RestrictionHandling.ALWAYS_CHECK || exclusionHandling == ExclusionHandling.ALWAYS_CHECK) {
      requireNotNull(deliusUsername) { "Delius username is required when applying LAO checks" }
    }
  }

  enum class RestrictionHandling {
    ALWAYS_CHECK,
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
    val NeverRestricted = LaoStrategy(
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

  fun ignoreAll() = restrictionHandling == RestrictionHandling.IGNORE && exclusionHandling == ExclusionHandling.IGNORE

  /**
   * The CaseSummary indicates if any restrictions/exclusions exist on the CRN,
   * but doesn't indicate if they apply to this specific user
   *
   * Depending on the LaoStrategy, the CaseSummary may be sufficient to determine access
   */
  fun canView(caseSummary: CaseSummary): CaseSummaryViewResult {
    val noApplicableExclusion = if (!caseSummary.currentExclusion) {
      true
    } else {
      when (exclusionHandling) {
        ExclusionHandling.ALWAYS_CHECK -> null
        ExclusionHandling.IGNORE -> true
      }
    }

    val noApplicableRestriction = if (!caseSummary.currentRestriction) {
      true
    } else {
      when (restrictionHandling) {
        RestrictionHandling.ALWAYS_CHECK -> null
        RestrictionHandling.IGNORE -> true
      }
    }

    return if (noApplicableExclusion == true && noApplicableRestriction == true) {
      CaseSummaryViewResult.CAN_VIEW
    } else {
      CaseSummaryViewResult.CHECK_CASE_ACCESS
    }
  }

  /**
   * CaseAccess indicates if there are any restrictions/exclusions that apply to a specific user.
   */
  fun canView(caseAccess: CaseAccess): Boolean {
    val excluded = when (exclusionHandling) {
      ExclusionHandling.ALWAYS_CHECK -> caseAccess.userExcluded
      ExclusionHandling.IGNORE -> false
    }

    val restricted = when (restrictionHandling) {
      RestrictionHandling.ALWAYS_CHECK -> caseAccess.userRestricted
      RestrictionHandling.IGNORE -> false
    }

    return !excluded && !restricted
  }

  enum class CaseSummaryViewResult {
    CAN_VIEW,
    CHECK_CASE_ACCESS,
  }
}

/**
 * This strategy is _not_ applied when creating an application. In that case, use cas1CreateApplicationLaoStrategy, which
 * always applies [LaoStrategy.checkUserAccess]
 */
fun UserEntity.cas1LaoStrategy() = if (this.hasQualification(UserQualification.LAO)) {
  LaoStrategy.NeverRestricted
} else {
  LaoStrategy.checkUserAccess(this.deliusUsername)
}

fun UserEntity.cas1CreateApplicationLaoStrategy() = LaoStrategy.checkUserAccess(this.deliusUsername)

fun UserEntity.cas3LaoStrategy() = LaoStrategy.checkUserAccess(this.deliusUsername)
