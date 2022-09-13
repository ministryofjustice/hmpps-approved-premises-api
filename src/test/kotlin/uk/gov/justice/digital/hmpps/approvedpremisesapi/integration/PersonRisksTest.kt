package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRisksClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisksSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PersonRisksTest : IntegrationTestBase() {
  @Test
  fun `Getting risks by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/risks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting risks for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN that does not exist returns 404`() {
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
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting risks for a CRN returns OK with correct body`() {
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
      get(WireMock.urlEqualTo("/risks/crn/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                RoshRisksClientResponseFactory().withSummary(
                  RoshRisksSummary(
                    whoIsAtRisk = null,
                    natureOfRisk = null,
                    riskImminence = null,
                    riskIncreaseFactors = null,
                    riskMitigationFactors = null,
                    riskInCommunity = mapOf(
                      RiskLevel.LOW to listOf("Children"),
                      RiskLevel.MEDIUM to listOf("Public"),
                      RiskLevel.HIGH to listOf("Known Adult"),
                      RiskLevel.VERY_HIGH to listOf("Staff")
                    ),
                    riskInCustody = mapOf(),
                    assessedOn = LocalDateTime.parse("2022-09-06T13:45:00"),
                    overallRiskLevel = RiskLevel.MEDIUM
                  )
                ).produce()
              )
            )
        )
    )

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/crn/CRN/tier"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                Tier(
                  tierScore = "M2",
                  calculationId = UUID.randomUUID(),
                  calculationDate = LocalDateTime.parse("2022-09-06T14:59:00")
                )
              )
            )
        )
    )

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/CRN/registrations?activeOnly=true"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                Registrations(
                  registrations = listOf(
                    RegistrationClientResponseFactory()
                      .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
                      .withRegisterCategory(RegistrationKeyValue(code = "C1", description = "C1"))
                      .withRegisterLevel(RegistrationKeyValue(code = "L1", description = "L1"))
                      .withStartDate(LocalDate.parse("2022-09-06"))
                      .produce(),
                    RegistrationClientResponseFactory()
                      .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
                      .produce()
                  )
                )
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
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          PersonRisks(
            crn = "CRN",
            roshRisks = RoshRisksEnvelope(
              status = RiskEnvelopeStatus.retrieved,
              value = RoshRisks(
                overallRisk = "Medium",
                riskToChildren = "Low",
                riskToPublic = "Medium",
                riskToKnownAdult = "High",
                riskToStaff = "Very High",
                lastUpdated = LocalDate.parse("2022-09-06")
              )
            ),
            tier = RiskTierEnvelope(
              status = RiskEnvelopeStatus.retrieved,
              value = RiskTier(
                level = "M2",
                lastUpdated = LocalDate.parse("2022-09-06")
              )
            ),
            flags = FlagsEnvelope(
              status = RiskEnvelopeStatus.retrieved,
              value = listOf("RISK FLAG")
            ),
            mappa = MappaEnvelope(
              status = RiskEnvelopeStatus.retrieved,
              value = Mappa(
                level = "CAT C1/LEVEL L1",
                lastUpdated = LocalDate.parse("2022-09-06")
              )
            )
          )
        )
      )
  }
}
