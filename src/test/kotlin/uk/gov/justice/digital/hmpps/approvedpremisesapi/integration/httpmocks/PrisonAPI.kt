package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

fun IntegrationTestBase.PrisonAPI_mockSuccessfulInmateDetailsCall(inmateDetail: InmateDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/${inmateDetail.offenderNo}",
    responseBody = inmateDetail
  )

fun IntegrationTestBase.PrisonAPI_mockNotFoundInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 404
  )
