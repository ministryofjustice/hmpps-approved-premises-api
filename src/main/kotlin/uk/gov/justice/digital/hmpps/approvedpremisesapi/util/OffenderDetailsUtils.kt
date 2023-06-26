package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

fun getNameFromOffenderDetailSummaryResult(result: AuthorisableActionResult<OffenderDetailSummary>) = when (result) {
  is AuthorisableActionResult.Success -> {
    "${result.entity.firstName} ${result.entity.surname}"
  }
  is AuthorisableActionResult.NotFound -> "Unknown"
  is AuthorisableActionResult.Unauthorised -> "LAO Offender"
}
