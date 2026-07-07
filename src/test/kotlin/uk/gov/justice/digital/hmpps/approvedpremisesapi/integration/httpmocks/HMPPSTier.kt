package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.hmppsTierMockSuccessfulTierCall(crn: String, response: Tier) = mockSuccessfulGetCallWithJsonResponse(
  url = "/crn/$crn/tier",
  responseBody = response,
)

fun IntegrationTestBase.hmppsTierMock404TierCall(crn: String) = mockSuccessfulGetCallWithJsonResponse(
  url = "/crn/$crn/tier",
  responseStatus = 404,
  responseBody = "",
)

fun IntegrationTestBase.hmppsTierMock500TierCall(crn: String) = mockSuccessfulGetCallWithJsonResponse(
  url = "/crn/$crn/tier",
  responseStatus = 500,
  responseBody = "",
)

fun IntegrationTestBase.hmppsTierMockSuccessfulV3TierCall(crn: String, response: Tier) = mockSuccessfulGetCallWithJsonResponse(
  url = "/v3/crn/$crn/tier",
  responseBody = response,
)

fun IntegrationTestBase.hmppsTierMock404V3TierCall(crn: String) = mockSuccessfulGetCallWithJsonResponse(
  url = "/v3/crn/$crn/tier",
  responseStatus = 404,
  responseBody = "",
)

fun IntegrationTestBase.hmppsTierMock500V3TierCall(crn: String) = mockSuccessfulGetCallWithJsonResponse(
  url = "/v3/crn/$crn/tier",
  responseStatus = 500,
  responseBody = "",
)
