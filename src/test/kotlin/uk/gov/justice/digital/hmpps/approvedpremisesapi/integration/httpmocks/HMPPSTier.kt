package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier

fun IntegrationTestBase.hmppsTierMockSuccessfulTierCall(crn: String, response: Tier) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/crn/$crn/tier",
    responseBody = response,
  )
