package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.adjudicationsAPIMockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) = mockSuccessfulGetCallWithJsonResponse(
  url = "/reported-adjudications/prisoner/$nomsNumber",
  responseBody = response,
)
