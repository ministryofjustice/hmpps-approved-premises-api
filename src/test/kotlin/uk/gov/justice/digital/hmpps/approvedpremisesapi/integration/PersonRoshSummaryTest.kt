package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoshSummaryTransformer

class PersonRoshSummaryTest : IntegrationTestBase() {
  @Autowired
  lateinit var roshSummaryTransformer: RoshSummaryTransformer

  @Test
  fun `Getting rosh summary by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/rosh-summary")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting rosh summary for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh-summary")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting rosh summary for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh-summary")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting rosh summary for a CRN that does not exist returns 404`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404)
        )
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh-summary")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting rosh summary for a CRN returns OK with correct body`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                OffenderDetailsSummaryFactory()
                  .withCrn("CRN")
                  .withFirstName("James")
                  .withLastName("Someone")
                  .produce()
              )
            )
        )
    )

    val riskManagementPlan = RoshSummaryFactory().apply {
      withAssessmentId(34853487)
      withWhoAtRisk("Who at Risk")
      withNatureOfRisk("Nature of Risk")
      withRiskGreatest("Risk Greatest")
      withRiskIncreaseLikelyTo("Increase Likely To")
      withRiskReductionLikelyTo("Reduction Likely To")
    }.produce()

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/rosh-summary/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                riskManagementPlan
              )
            )
        )
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh-summary")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          roshSummaryTransformer.transformToApi(riskManagementPlan)
        )
      )
  }
}
