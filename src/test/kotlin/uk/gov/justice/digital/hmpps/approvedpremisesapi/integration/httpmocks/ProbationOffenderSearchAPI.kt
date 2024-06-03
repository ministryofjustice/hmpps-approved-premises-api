package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderSearchNomsRequest

fun IntegrationTestBase.probationOffenderSearchApiMockSuccessfulOffenderSearchCall(nomsNumber: String, response: List<ProbationOffenderDetail?>) =
  mockSuccessfulPostCallWithJsonResponse(
    url = "/search",
    requestBody = WireMock.equalToJson(
      objectMapper.writeValueAsString(
        ProbationOffenderSearchNomsRequest(nomsNumber = nomsNumber),
      ),
      true,
      true,
    ),
    responseBody = response,
  )

fun IntegrationTestBase.probationOffenderSearchApiMockForbiddenOffenderSearchCall() =
  mockUnsuccessfulPostCall(
    url = "/search",
    responseStatus = 403,
  )

fun IntegrationTestBase.probationOffenderSearchApiMockServerErrorSearchCall() =
  mockUnsuccessfulPostCall(
    url = "/search",
    responseStatus = 500,
  )

fun IntegrationTestBase.probationOffenderSearchApiMockNotFoundSearchCall() =
  mockUnsuccessfulPostCall(
    url = "/search",
    responseStatus = 404,
  )
