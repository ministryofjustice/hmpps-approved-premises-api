package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail

fun IntegrationTestBase.NomisUserRoles_mockSuccessfulGetUserDetailsCall(
  jwt: String,
  nomisUserDetails: NomisUserDetail,
) =
  wiremockServer.stubFor(
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
