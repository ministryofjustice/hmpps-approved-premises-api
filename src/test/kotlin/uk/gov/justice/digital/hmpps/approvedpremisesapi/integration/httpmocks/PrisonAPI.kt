package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.AlertsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CsraSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.PrisonerInPrisonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail: InmateDetail) = mockSuccessfulGetCallWithJsonResponse(
  url = "/api/offenders/${inmateDetail.offenderNo}",
  responseBody = inmateDetail,
)

fun IntegrationTestBase.prisonAPIMockNotFoundInmateDetailsCall(offenderNo: String) = mockUnsuccessfulGetCall(
  url = "/api/offenders/$offenderNo",
  responseStatus = 404,
)

fun IntegrationTestBase.prisonAPIMockServerErrorInmateDetailsCall(offenderNo: String) = mockUnsuccessfulGetCall(
  url = "/api/offenders/$offenderNo",
  responseStatus = 500,
)

fun IntegrationTestBase.prisonerAlertsAPIMockSuccessfulAlertsCall(nomsNumber: String, alertCode: String, alertsPage: AlertsPage) = mockSuccessfulGetCallWithJsonResponse(
  url = "/prisoners/$nomsNumber/alerts?alertCode=$alertCode&sort=createdAt,DESC",
  responseBody = alertsPage,
)

fun IntegrationTestBase.prisonAPIMockSuccessfulPrisonTimeLineCall(nomsNumber: String, summary: PrisonerInPrisonSummary) = mockSuccessfulGetCallWithJsonResponse(
  url = "/api/offenders/$nomsNumber/prison-timeline",
  responseBody = summary,
)

fun IntegrationTestBase.prisonAPIMockNotFoundPrisonTimeLineCall(nomsNumber: String) = mockUnsuccessfulGetCall(
  url = "/api/offenders/$nomsNumber/prison-timeline",
  responseStatus = 404,
)

fun IntegrationTestBase.prisonAPIMockServerErrorPrisonTimeLineCall(nomsNumber: String) = mockUnsuccessfulGetCall(
  url = "/api/offenders/$nomsNumber/prison-timeline",
  responseStatus = 500,
)

fun IntegrationTestBase.prisonAPIMockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) = mockSuccessfulGetCallWithJsonResponse(
  url = "/api/offenders/$nomsNumber/adjudications",
  responseBody = response,
)

fun IntegrationTestBase.prisonAPIMockSuccessfulAgencyDetailsCall(agency: Agency) = mockSuccessfulGetCallWithJsonResponse(
  url = "/api/agencies/${agency.agencyId}",
  responseBody = agency,
)

fun IntegrationTestBase.prisonAPIMockSuccessfulCsraSummariesCall(nomsNumber: String, summaries: List<CsraSummary>) = mockSuccessfulGetCallWithJsonResponse(
  url = "/api/offender-assessments/csra/$nomsNumber",
  responseBody = summaries,
)

fun IntegrationTestBase.prisonAPIMockNotFoundCsraSummariesCall(nomsNumber: String) = mockUnsuccessfulGetCall(
  url = "/api/offender-assessments/csra/$nomsNumber",
  responseStatus = 404,
)

fun IntegrationTestBase.prisonAPIMockServerErrorCsraSummariesCall(nomsNumber: String) = mockUnsuccessfulGetCall(
  url = "/api/offender-assessments/csra/$nomsNumber",
  responseStatus = 500,
)
