package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient.Companion.MAX_CASE_ACCESS_REQUEST_COUNT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InternalServerErrorProblem

@Service
class LaoService(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) {

  /**
   * Determines visibility for a list of [CaseSummary]'s, given an [LaoStrategy].
   *
   * If the caller already has/requires a list of [CaseSummary]'s this function
   * should be used in preference to [isVisibleForCrns] because typically
   * the [CaseSummary] is sufficient to determine access without having
   * to retrieve [UserAccess] details (e.g. if a given [CaseSummary] indicates
   * no exclusions/restrictions exist)
   *
   * When [UserAccess] details _are_ required to determine visibility, they
   * will only be fetched for the required  CRNs
   *
   * Will return an entry for each provided CRN
   */
  fun isVisibleForCaseSummaries(
    caseSummaries: List<CaseSummary>,
    laoStrategy: LaoStrategy,
  ): List<IsVisible> {
    if (caseSummaries.isEmpty()) return emptyList()

    val visibilityFromCaseSummary = visibilityFromCaseSummary(
      caseSummaries = caseSummaries,
      laoStrategy = laoStrategy,
    )

    val indeterminateVisibility = visibilityFromCaseSummary.filter { !it.couldDetermineVisibility() }

    val visibilityFromCaseAccess = isVisibleForCrns(
      crns = indeterminateVisibility.map { it.caseSummary.crn },
      laoStrategy = laoStrategy,
    )

    return (
      visibilityFromCaseSummary.filter { it.couldDetermineVisibility() }.map { it.toIsVisible() } +
        visibilityFromCaseAccess.map { it.toIsVisible(caseSummaries) }
      )
  }

  /**
   * Will fetch [UserAccess] information for each given CRN and determine if
   * the Case is visible, for the given [LaoStrategy]
   *
   * If the caller already has/requires a list of [CaseSummary]'s, use [isVisibleForCaseSummaries]
   * instead because it's more efficient
   *
   * Will return an entry for each provided CRN
   */
  fun isVisibleForCrns(
    crns: List<String>,
    laoStrategy: LaoStrategy,
  ): List<VisibilityFromCaseAccess> {
    if (crns.isEmpty()) return emptyList()

    if (crns.size > MAX_CASE_ACCESS_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request access details for more than $MAX_CASE_ACCESS_REQUEST_COUNT CRNs. ${crns.size} have been provided.")
    }

    if (laoStrategy.isNeverRestricted()) {
      return crns.map {
        VisibilityFromCaseAccess(
          crn = it,
          laoStrategy = laoStrategy,
          isVisible = true,
        )
      }
    }

    val caseAccess = when (
      val result = apDeliusContextApiClient.getUserAccessForCrns(
        deliusUsername = laoStrategy.deliusUsername!!,
        crns = crns,
      )
    ) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val accessByCrn = caseAccess.access.associateBy(
      keySelector = { it.crn },
      valueTransform = { it },
    )

    return crns.map { crn ->
      val access = accessByCrn[crn] ?: error("No case access found for CRN $crn")

      VisibilityFromCaseAccess(
        crn = crn,
        laoStrategy = laoStrategy,
        isVisible = laoStrategy.canView(access),
      )
    }
  }

  private fun visibilityFromCaseSummary(
    caseSummaries: List<CaseSummary>,
    laoStrategy: LaoStrategy,
  ): List<VisibilityFromCaseSummary> = caseSummaries.map {
    VisibilityFromCaseSummary(
      caseSummary = it,
      laoStrategy = laoStrategy,
      isVisible = when (laoStrategy.canView(it)) {
        LaoStrategy.CaseSummaryViewResult.CAN_VIEW -> true
        LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS -> null
      },
    )
  }

  data class VisibilityFromCaseAccess(
    val crn: String,
    val laoStrategy: LaoStrategy,
    val isVisible: Boolean,
  ) {
    fun toIsVisible(caseSummaries: List<CaseSummary>) = IsVisible(
      caseSummary = caseSummaries.first { it.crn == crn },
      laoStrategy = laoStrategy,
      isVisible = isVisible,
    )
  }

  private data class VisibilityFromCaseSummary(
    val caseSummary: CaseSummary,
    val laoStrategy: LaoStrategy,
    /**
     * If non-null the case summary can be used to determine visibility
     * for the given laoStrategy
     */
    val isVisible: Boolean?,
  ) {
    fun couldDetermineVisibility() = isVisible != null

    fun toIsVisible() = IsVisible(
      caseSummary = caseSummary,
      laoStrategy = laoStrategy,
      isVisible = isVisible!!,
    )
  }

  data class IsVisible(
    val caseSummary: CaseSummary,
    val laoStrategy: LaoStrategy,
    val isVisible: Boolean,
  )
}
