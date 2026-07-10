package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.util.UUID

fun IntegrationTestBase.cas1UiMockPostForBackfillApplicationDocument(
  applicationId: UUID,
  request: String,
  response: String,
) = mockSuccessfulPostCallWithJsonStringResponse(
  url = "/backfill/application/$applicationId",
  requestBody = WireMock.equalToJson(request),
  responseBody = response,
)
