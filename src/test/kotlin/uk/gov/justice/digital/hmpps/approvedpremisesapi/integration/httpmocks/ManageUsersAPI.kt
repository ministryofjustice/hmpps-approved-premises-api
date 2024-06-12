package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.manageusers.ExternalUserDetails

fun IntegrationTestBase.ManageUsers_mockSuccessfulExternalUsersCall(
  username: String,
  externalUserDetails: ExternalUserDetails,
) =
  wiremockServer.stubFor(
    WireMock.get(urlEqualTo("/externalusers/$username"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(externalUserDetails),
          ),
      ),
  )
