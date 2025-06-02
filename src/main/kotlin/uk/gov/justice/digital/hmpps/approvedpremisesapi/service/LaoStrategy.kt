package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderService

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
 * Always return an Offender as [PersonInfoResult.Success.Restricted] if a restriction or exclusion exists,
 * regardless of the users it applies to. This is because CAS1 is only used by NOMIS and External users.
 *
 * Implemented in [Cas2OffenderService]
 *
 * # CAS2 Bail
 *
 * If a restriction exists on the offender (regardless of whether it applies to the user), return [PersonInfoResult.Success.Restricted]
 * If an exclusion exists on the offender, return the full offender info
 *
 * This logic has been applied because CAS1 is used by NOMIS, External and Delius users, so we can't always check restrictions.
 *
 * We _could_ tighten this up for Delius users
 *
 * Implemented in [Cas2v2OffenderService]
 *
 * # CAS3
 *
 * See [cas3LaoStrategy()] below
 */
sealed interface LaoStrategy {
  /**
   * Even if the calling user has exclusions or restrictions for a given offender,
   * return [PersonSummaryInfoResult.Success.Full] or [PersonInfoResult.Success.Full] regardless.
   *
   * This strategy should be used with care, typically when the calling user
   * has a specific qualification that indicates they can always view limited
   * access offender information
   */
  data object NeverRestricted : LaoStrategy

  /**
   * If the offender is LAO, retrieve the access information for the calling delius user.
   * If the user has limited access to this offender (either restricted or excluded), return
   * [PersonInfoResult.Success.Restricted] or [PersonSummaryInfoResult.Success.Restricted]
   */
  data class CheckUserAccess(val deliusUsername: String) : LaoStrategy
}

/**
 * This strategy is _not_ applied when creating an application. In that case, use cas1CreateApplicationLaoStrategy, which
 * always applies [LaoStrategy.CheckUserAccess]
 */
fun UserEntity.cas1LaoStrategy() = if (this.hasQualification(UserQualification.LAO)) {
  LaoStrategy.NeverRestricted
} else {
  LaoStrategy.CheckUserAccess(this.deliusUsername)
}

fun UserEntity.cas1CreateApplicationLaoStrategy() = LaoStrategy.CheckUserAccess(this.deliusUsername)

fun UserEntity.cas3LaoStrategy() = LaoStrategy.CheckUserAccess(this.deliusUsername)
