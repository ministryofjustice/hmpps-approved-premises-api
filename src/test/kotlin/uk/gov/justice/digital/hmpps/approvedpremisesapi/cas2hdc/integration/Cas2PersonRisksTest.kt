package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.RiskEnvelopeStatusDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.RiskTierEnvelopeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import java.time.OffsetDateTime

class Cas2PersonRisksTest : IntegrationTestBase() {
  @Test
  fun `Getting risks by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/risks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting risks for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN without ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "nomis",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN that does not exist returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      val crn = "CRN123"

      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      webTestClient.get()
        .uri("/cas2-hdc/people/$crn/risks")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting risks for a CRN returns OK with correct body`() {
    val dateTimeCompleted = OffsetDateTime.now().minusMonths(1)
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        apAndOASysMockSuccessfulRoshRatingsCall(
          offenderDetails.otherIds.crn,
          RoshRatingsFactory().apply {
            withDateCompleted(dateTimeCompleted)
            withAssessmentId(34853487)
            withRiskChildrenCommunity(RiskLevel.LOW)
            withRiskPublicCommunity(RiskLevel.MEDIUM)
            withRiskKnownAdultCommunity(RiskLevel.HIGH)
            withRiskStaffCommunity(RiskLevel.VERY_HIGH)
          }.produce(),
        )

        webTestClient.get()
          .uri("/cas2-hdc/people/${offenderDetails.otherIds.crn}/risks")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
              PersonRisks(
                crn = offenderDetails.otherIds.crn,
                roshRisks = RoshRisksEnvelope(
                  status = RiskEnvelopeStatusDto.retrieved,
                  value = RoshRisks(
                    overallRisk = "Very High",
                    riskToChildren = "Low",
                    riskToPublic = "Medium",
                    riskToKnownAdult = "High",
                    riskToStaff = "Very High",
                    lastUpdated = dateTimeCompleted.toLocalDate(),
                  ),
                ),
                tier = RiskTierEnvelopeDto(
                  status = RiskEnvelopeStatusDto.notFound,
                  value = null,
                ),
                flags = FlagsEnvelope(
                  status = RiskEnvelopeStatusDto.notFound,
                  value = null,
                ),
                mappa = MappaEnvelope(
                  status = RiskEnvelopeStatusDto.notFound,
                  value = null,
                ),
              ),
            ),
          )
      }
    }
  }
}
