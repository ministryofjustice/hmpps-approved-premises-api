package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.licenceApiMockSuccessfulLicenceSummaries(
  crn: String,
  summaries: List<LicenceSummary>,
) = mockSuccessfulGetCallWithJsonResponse(
  url = "/public/licence-summaries/crn/$crn",
  responseBody = summaries,
)

fun IntegrationTestBase.licenceApiMockNotFoundLicenceSummaries(crn: String) = mockUnsuccessfulGetCall(
  url = "/public/licence-summaries/crn/$crn",
  responseStatus = 404,
)

fun IntegrationTestBase.licenceApiMockServerErrorLicenceSummaries(crn: String) = mockUnsuccessfulGetCall(
  url = "/public/licence-summaries/crn/$crn",
  responseStatus = 500,
)

fun IntegrationTestBase.licenceApiMockSuccessfulLicenceDetails(licence: Licence) = mockSuccessfulGetCallWithJsonResponse(
  url = "/public/licences/id/${licence.id}",
  responseBody = licence,
)

fun IntegrationTestBase.licenceApiMockNotFoundLicenceDetails(licenceId: Long) = mockUnsuccessfulGetCall(
  url = "/public/licences/id/$licenceId",
  responseStatus = 404,
)

fun IntegrationTestBase.licenceApiMockServerErrorLicenceDetails(licenceId: Long) = mockUnsuccessfulGetCall(
  url = "/public/licences/id/$licenceId",
  responseStatus = 500,
)
