package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsPage

fun IntegrationTestBase.AdjudicationsAPI_mockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/adjudications/$nomsNumber/adjudications",
    responseBody = response,
  )
