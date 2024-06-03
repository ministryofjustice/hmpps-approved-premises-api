package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonerInPrisonSummary

fun IntegrationTestBase.prisonApiMockSuccessfulInmateDetailsCall(inmateDetail: InmateDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/${inmateDetail.offenderNo}",
    responseBody = inmateDetail,
  )

fun IntegrationTestBase.prisonApiMockNotFoundInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 404,
  )

fun IntegrationTestBase.prisonApiMockServerErrorInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 500,
  )

fun IntegrationTestBase.prisonApiMockSuccessfulAlertsCall(nomsNumber: String, alerts: List<Alert>) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/alerts/v2?alertCodes=HA&sort=dateCreated&direction=DESC",
    responseBody = alerts,
  )

fun IntegrationTestBase.prisonApiMockSuccessfulPrisonTimeLineCall(nomsNumber: String, summary: PrisonerInPrisonSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseBody = summary,
  )

fun IntegrationTestBase.prisonApiMockNotFoundPrisonTimeLineCall(nomsNumber: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseStatus = 404,
  )

fun IntegrationTestBase.prisonApiMockServerErrorPrisonTimeLineCall(nomsNumber: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseStatus = 500,
  )

fun IntegrationTestBase.prisonApiMockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/adjudications",
    responseBody = response,
  )
