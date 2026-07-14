package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.cas1UiMockRenderApplicationPost(
  request: String,
  response: String,
) = mockSuccessfulPostCallWithJsonStringResponse(
  url = "/render-application",
  requestBody = WireMock.equalToJson(request),
  responseBody = response,
)

fun IntegrationTestBase.cas1UiMockRenderApplicationPost500Error(
  request: String,
  response: String,
) = mockSuccessfulPostCallWithJsonStringResponse(
  url = "/render-application",
  requestBody = WireMock.equalToJson(request),
  responseBody = response,
  responseStatus = 500,
)
