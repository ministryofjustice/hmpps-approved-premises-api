package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary

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
 * Note that there is a 3rd strategy not explicit mentioned here, used by CAS2 via
 * the [uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService].
 * This is used for calls originating from NOMIS and External users and will always
 * return an Offender as [PersonInfoResult.Success.Restricted] if a restriction or
 * exclusion exists. We should consider adding this here as an `AlwaysRestricted` strategy
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
 * If the user has the `LAO` qualification, they can always view LAO offenders
 *
 * Note that there are some cases in CAS1 where this strategy should not be used
 * (e.g. when creating applications the LAO qualification should be ignored)
 */
fun UserEntity.cas1LaoStrategy() = if (this.hasQualification(UserQualification.LAO)) {
  LaoStrategy.NeverRestricted
} else {
  LaoStrategy.CheckUserAccess(this.deliusUsername)
}

fun Cas2v2UserEntity.cas2DeliusUserLaoStrategy() = when (userType) {
  Cas2v2UserType.DELIUS -> LaoStrategy.CheckUserAccess(this.username)
  else -> error("Can't provide strategy for users of type $userType")
}

fun UserEntity.cas3LaoStrategy() = LaoStrategy.CheckUserAccess(this.deliusUsername)
