package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

fun IntegrationTestBase.PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetail: InmateDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/${inmateDetail.offenderNo}",
    responseBody = inmateDetail,
  )

fun IntegrationTestBase.PrisonAPI_mockNotFoundInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 404,
  )

fun IntegrationTestBase.PrisonAPI_mockSuccessfulAlertsCall(nomsNumber: String, alerts: List<Alert>) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/alerts/v2?alertCodes=HA&sort=dateCreated&direction=DESC",
    responseBody = alerts,
  )

fun IntegrationTestBase.PrisonAPI_mockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/adjudications",
    responseBody = response,
  )
