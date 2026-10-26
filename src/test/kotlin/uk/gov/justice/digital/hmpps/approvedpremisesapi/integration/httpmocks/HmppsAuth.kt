package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.RequestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppsauth.GetTokenResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.time.Duration
import java.util.UUID

fun IntegrationTestBase.isClientCredentialsJwtRequestMocked() = wiremockServer.listAllStubMappings().mappings.any {
  it.request.method == RequestMethod.POST && it.request.url == "/auth/oauth/token"
}

fun IntegrationTestBase.hmppsAuthMockClientCredentialsJwtRequest(
  username: String? = null,
  roles: List<String> = listOf(),
  authSource: String = "none",
) {
  wiremockServer.stubFor(
    post(urlEqualTo("/auth/oauth/token"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            jsonMapper.writeValueAsString(
              GetTokenResponse(
                accessToken = jwtAuthHelper.createClientCredentialsJwt(
                  username = username,
                  roles = roles,
                  authSource = authSource,
                ),
                tokenType = "bearer",
                expiresIn = Duration.ofHours(1).toSeconds().toInt(),
                scope = "read",
                sub = username?.uppercase() ?: "integration-test-client-id",
                authSource = authSource,
                jti = UUID.randomUUID().toString(),
                iss = "http://localhost:${wiremockServer.port()}/auth/issuer",
              ),
            ),
          ),
      ),
  )
}
