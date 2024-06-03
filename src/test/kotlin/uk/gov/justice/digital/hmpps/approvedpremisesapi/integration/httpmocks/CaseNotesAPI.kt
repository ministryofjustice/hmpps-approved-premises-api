package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun IntegrationTestBase.caseNotesAPIMockSuccessfulCaseNotesCall(page: Int, from: LocalDate, nomsNumber: String, result: CaseNotesPage) {
  val fromLocalDateTime = LocalDateTime.of(from, LocalTime.MIN)

  mockSuccessfulGetCallWithJsonResponse(
    url = "/case-notes/$nomsNumber?startDate=$fromLocalDateTime&page=$page&size=30",
    responseBody = result,
  )
}
