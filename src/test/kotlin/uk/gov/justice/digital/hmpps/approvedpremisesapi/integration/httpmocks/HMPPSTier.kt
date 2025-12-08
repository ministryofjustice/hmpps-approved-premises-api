package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.hmppsTierMockSuccessfulTierCall(crn: String, response: Tier) = mockSuccessfulGetCallWithJsonResponse(
  url = "/crn/$crn/tier",
  responseBody = response,
)

fun IntegrationTestBase.hmppsTierMockUnsuccessfulTierCall(crn: String, responseStatus: Int = 500) = mockUnsuccessfulGetCall(
  url = "/crn/$crn/tier",
  responseStatus = responseStatus,
)
