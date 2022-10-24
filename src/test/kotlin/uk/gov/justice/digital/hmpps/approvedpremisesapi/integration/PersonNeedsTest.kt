package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonNeed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonNeeds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.NeedSeverity
import java.time.LocalDateTime

class PersonNeedsTest : IntegrationTestBase() {
  @Test
  fun `Getting needs by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/needs")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting needs for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/needs")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting needs for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/needs")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting needs for a CRN that does not exist returns 404`() {
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
      .uri("/people/CRN/needs")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting needs for a CRN returns OK with correct body`() {
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

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/needs/crn/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                NeedsFactory()
                  .withAssessedOn(LocalDateTime.now())
                  .withIdentifiedNeeds(
                    listOf(
                      NeedFactory()
                        .withSeverity(NeedSeverity.SEVERE)
                        .withSection("SEVERE_HARM")
                        .withName("SEVERE_HARM")
                        .withOverThreshold(true)
                        .withRiskOfHarm(true)
                        .withRiskOfReoffending(true)
                        .withFlaggedAsNeed(true)
                        .withIdentifiedAsNeed(true)
                        .withNeedScore(10)
                        .produce(),
                      NeedFactory()
                        .withSeverity(NeedSeverity.STANDARD)
                        .withSection("STANDARD_HARM")
                        .withName("STANDARD_HARM")
                        .withOverThreshold(true)
                        .withRiskOfHarm(true)
                        .withRiskOfReoffending(false)
                        .withFlaggedAsNeed(true)
                        .withIdentifiedAsNeed(true)
                        .withNeedScore(2)
                        .produce(),
                      NeedFactory()
                        .withSeverity(NeedSeverity.STANDARD)
                        .withSection("RISK_OF_REOFFEND")
                        .withName("RISK_OF_REOFFEND")
                        .withOverThreshold(true)
                        .withRiskOfHarm(false)
                        .withRiskOfReoffending(true)
                        .withFlaggedAsNeed(true)
                        .withIdentifiedAsNeed(true)
                        .withNeedScore(5)
                        .produce()
                    )
                  ).produce()
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
      .uri("/people/CRN/needs")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          PersonNeeds(
            linkedToRiskOfSeriousHarm = listOf(
              PersonNeed(
                section = "SEVERE_HARM",
                name = "SEVERE_HARM",
                overThreshold = true,
                riskOfHarm = true,
                flaggedAsNeed = true,
                severity = "SEVERE",
                identifiedAsNeed = true,
                needScore = 10,
                riskOfReoffending = true
              )
            ),
            linkedToReoffending = listOf(
              PersonNeed(
                section = "RISK_OF_REOFFEND",
                name = "RISK_OF_REOFFEND",
                overThreshold = true,
                riskOfHarm = false,
                flaggedAsNeed = true,
                severity = "STANDARD",
                identifiedAsNeed = true,
                needScore = 5,
                riskOfReoffending = true
              )
            ),
            notLinkedToSeriousHarmOrReoffending = listOf(
              PersonNeed(
                section = "STANDARD_HARM",
                name = "STANDARD_HARM",
                overThreshold = true,
                riskOfHarm = true,
                flaggedAsNeed = true,
                severity = "STANDARD",
                identifiedAsNeed = true,
                needScore = 2,
                riskOfReoffending = false
              )
            )
          )
        )
      )
  }
}
