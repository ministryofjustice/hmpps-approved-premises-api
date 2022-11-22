package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RiskToTheIndividualTransformer

class RisksToTheIndividualTest : IntegrationTestBase() {
  @Autowired
  lateinit var riskToTheIndividualTransformer: RiskToTheIndividualTransformer

  @Test
  fun `Getting risk to the individual by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/risks-to-the-individual")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting risk to the individual for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risks-to-the-individual")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risk to the individual for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risks-to-the-individual")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risk to the individual for a CRN that does not exist returns 404`() {
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
      .uri("/people/CRN/oasys/risks-to-the-individual")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting risk to the individual for a CRN returns OK with correct body`() {
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

    val riskToTheIndividual = RiskToTheIndividualFactory().apply {
      withAssessmentId(34853487)
      withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicide")
      withPreviousConcernsSelfHarmSuicide("previousConcernsSelfHarmSuicide")
      withCurrentCustodyHostelCoping("currentCustodyHostelCoping")
      withPreviousCustodyHostelCoping("previousCustodyHostelCoping")
      withCurrentVulnerability("currentVulnerability")
      withPreviousVulnerability("previousVulnerability")
      withRiskOfSeriousHarm("riskOfSeriousHarm")
      withCurrentConcernsBreachOfTrustText("currentConcernsBreachOfTrustText")
    }.produce()

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/risk-to-the-individual/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                riskToTheIndividual
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
      .uri("/people/CRN/oasys/risks-to-the-individual")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          riskToTheIndividualTransformer.transformToApi(riskToTheIndividual)
        )
      )
  }
}
