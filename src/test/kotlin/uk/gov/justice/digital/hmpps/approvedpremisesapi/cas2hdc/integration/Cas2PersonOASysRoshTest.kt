package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcOAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockUnsuccessfulRoshCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn

class Cas2PersonOASysRoshTest : IntegrationTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: Cas2HdcOAsysSectionsTransformer

  @Test
  fun `Getting RoSH by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/oasys/rosh")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting RoSH  for a CRN with an invalid auth-source JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "bananas",
    )

    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting RoSH for a CRN without ROLE_PROBATION or ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "nomis",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2-hdc/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting Rosh for a CRN that does not exist returns 404`() {
    givenACas2PomUser { _, jwt ->
      val crn = "CRN123"

      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      webTestClient.get()
        .uri("/cas2-hdc/people/$crn/oasys/rosh")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting RoSH for a CRN returns OK with correct body`() {
    givenACas2PomUser { _, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val rosh = RoshSummaryFactory().produce()
        apAndOASysMockSuccessfulRoSHSummaryCall(offenderDetails.otherIds.crn, rosh)

        webTestClient.get()
          .uri("/cas2-hdc/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
              oaSysSectionsTransformer.transformRiskOfSeriousHarm(
                rosh,
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting RoSH when upstream times out returns 404`() {
    givenACas2PomUser { _, jwt ->
      givenAnOffender { offenderDetails, _ ->
        apAndOASysMockUnsuccessfulRoshCallWithDelay(offenderDetails.otherIds.crn, 2500)

        webTestClient.get()
          .uri("/cas2-hdc/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
