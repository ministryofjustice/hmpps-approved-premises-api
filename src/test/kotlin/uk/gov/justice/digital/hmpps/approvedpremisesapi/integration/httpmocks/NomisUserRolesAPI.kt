package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail

fun IntegrationTestBase.nomisUserRolesMockSuccessfulGetMeCall(
  jwt: String,
  nomisUserDetails: NomisUserDetail,
) = wiremockServer.stubFor(
  WireMock.get(WireMock.urlEqualTo("/me"))
    .willReturn(
      WireMock.aResponse()
        .withHeader("Content-Type", "application/json")
        .withHeader("authorization", "Bearer $jwt")
        .withStatus(200)
        .withBody(
          objectMapper.writeValueAsString(nomisUserDetails),
        ),
    ),
)

fun IntegrationTestBase.nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall(staffId: Long, response: NomisStaffInformation, responseStatus: Int = 200) = mockSuccessfulGetCallWithJsonResponse(
  url = "/users/staff/$staffId",
  responseBody = response,
  responseStatus = responseStatus,
)

fun IntegrationTestBase.nomisUserRolesMockSuccessfulGetUserByUsernameCall(username: String, response: NomisUserDetail, responseStatus: Int = 200) = mockSuccessfulGetCallWithJsonResponse(
  url = "/users/$username",
  responseBody = response,
  responseStatus = responseStatus,
)
