package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.healthAndMedicationMockSuccessfulDietAndAllergyCall(prisonerNumber: String, response: DietAndAllergyResponse) = mockSuccessfulGetCallWithJsonResponse(
  url = "/prisoners/$prisonerNumber",
  responseBody = response,
)

fun IntegrationTestBase.healthAndMedicationMockNotFoundDietAndAllergyCall(prisonerNumber: String) = mockUnsuccessfulGetCall(
  url = "/prisoners/$prisonerNumber",
  responseStatus = 404,
)

fun IntegrationTestBase.healthAndMedicationMockForbiddenDietAndAllergyCall(prisonerNumber: String) = mockUnsuccessfulGetCall(
  url = "/prisoners/$prisonerNumber",
  responseStatus = 403,
)
