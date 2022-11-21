package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RiskManagementPlanTransformer

class PersonRiskManagementPlanTest : IntegrationTestBase() {
  @Autowired
  lateinit var riskManagementPlanTransformer: RiskManagementPlanTransformer

  @Test
  fun `Getting risk management plan by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/risk-management-plan")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting risk management plan for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risk-management-plan")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risk management plan for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risk-management-plan")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risk management plan for a CRN that does not exist returns 404`() {
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
      .uri("/people/CRN/oasys/risk-management-plan")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting risk management plan for a CRN returns OK with correct body`() {
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

    val riskManagementPlan = RiskManagementPlanFactory().apply {
      withAssessmentId(34853487)
      withFurtherConsiderations("Further Considerations")
      withAdditionalComments("Additional Comments")
      withContingencyPlans("Contingency Plans")
      withVictimSafetyPlanning("Victim Safety Planning")
      withInterventionsAndTreatment("Interventions and Treatment")
      withMonitoringAndControl("Monitoring and Control")
      withSupervision("Supervision")
      withKeyInformationAboutCurrentSituation("Key Information About Current Situation")
    }.produce()

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/risk-management-plan/CRN"))
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
      .uri("/people/CRN/oasys/risk-management-plan")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          riskManagementPlanTransformer.transformToApi(riskManagementPlan)
        )
      )
  }
}
