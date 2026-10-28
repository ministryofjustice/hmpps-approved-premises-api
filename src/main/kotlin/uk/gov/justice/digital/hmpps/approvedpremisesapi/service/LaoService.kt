package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient.Companion.MAX_CASE_ACCESS_REQUEST_COUNT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CaseSummaryViewResult

@Service
class LaoService(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) {

  /**
   * Determines visibility for a list of [CaseSummary]'s, given an [LaoStrategy].
   *
   * If the caller already has a list of [CaseSummary]'s this function should be used in preference to [isVisible]
   * because typically the [CaseSummary] is sufficient to determine access without having to retrieve [CaseAccess] details
   * (e.g. if a given [CaseSummary] indicates no exclusions/restrictions exist)
   *
   * When [CaseAccess] details _are_ required to determine visibility, they will only be fetched for the corresponding CRNs
   *
   * Will return an entry for each provided CRN
   */
  fun isVisibleForCaseSummaries(
    caseSummaries: List<CaseSummary>,
    laoStrategy: LaoStrategy,
  ): Map<String, Boolean> {
    if (caseSummaries.isEmpty()) return emptyMap()

    val crnToCaseSummaryViewResult = caseSummaries.associate {
      it.crn to laoStrategy.canView(it)
    }

    val visibilityFromCaseSummary = crnToCaseSummaryViewResult
      .filter { it.value != CaseSummaryViewResult.CHECK_CASE_ACCESS }
      .mapValues { it.value.toIsVisible() }

    val visibilityFromCaseAccess = isVisible(
      crns = crnToCaseSummaryViewResult.filter { it.value == CaseSummaryViewResult.CHECK_CASE_ACCESS }.keys.toList(),
      laoStrategy = laoStrategy,
    )

    return visibilityFromCaseSummary + visibilityFromCaseAccess
  }

  private fun CaseSummaryViewResult.toIsVisible() = when (this) {
    CaseSummaryViewResult.CAN_VIEW -> true
    CaseSummaryViewResult.CANNOT_VIEW -> false
    CaseSummaryViewResult.CHECK_CASE_ACCESS -> error("cannot determine visibility")
  }

  /**
   * Will fetch [CaseAccess] information for each given CRN and determine if the Case is visible, given an [LaoStrategy]
   *
   * If the caller already has a list of [CaseSummary]'s, use [isVisibleForCaseSummaries] instead because it's more efficient\
   * as in many cases visibility can be determined by the [CaseSummary] alone
   *
   * Will return an entry for each provided CRN
   */
  fun isVisible(
    crns: List<String>,
    laoStrategy: LaoStrategy,
  ): Map<String, Boolean> {
    if (crns.isEmpty()) return emptyMap()

    if (crns.size > MAX_CASE_ACCESS_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request access details for more than $MAX_CASE_ACCESS_REQUEST_COUNT CRNs. ${crns.size} have been provided.")
    }

    if (laoStrategy.isNeverRestricted()) {
      return crns.associateWith { true }
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

    return crns.associateWith { crn ->
      val access = accessByCrn[crn] ?: error("No case access found for CRN $crn")

      laoStrategy.canView(access)
    }
  }
}
