package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.caseNotesAPIMockSuccessfulCaseNotesCall(personIdentifier: String, request: CaseNotesRequest, result: CaseNotesPage) {
  val requestBodyString = objectMapper.writeValueAsString(request)

  mockSuccessfulPostCallWithJsonResponse(
    url = "/search/case-notes/$personIdentifier",
    responseBody = result,
    requestBody = equalToJson(requestBodyString, true, true),
  )
}
