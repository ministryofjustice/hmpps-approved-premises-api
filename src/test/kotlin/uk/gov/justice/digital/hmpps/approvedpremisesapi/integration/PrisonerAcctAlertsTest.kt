package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.prisoneralerts.AlertsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonerAlertsAPIMockSuccessfulAlertsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonerAlertTransformer

class PrisonerAcctAlertsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var prisonerAlertTransformer: PrisonerAlertTransformer

  @Test
  fun `Getting ACCT alerts by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting ACCT alerts for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting ACCT alerts for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting ACCT alerts for a CRN that does not exist returns 404`() {
    givenAUser { _, jwt ->
      val crn = "CRN123"
      apDeliusContextCaseSummariesEmptyResponseForCrn(crn)

      webTestClient.get()
        .uri("/people/$crn/acct-alerts")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting ACCT alerts for a CRN returns OK with correct body`() {
    givenAUser { _, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val alerts = AlertsPageFactory().produce()

        prisonerAlertsAPIMockSuccessfulAlertsCall(offenderDetails.otherIds.nomsNumber!!, "HA", alerts)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
              alerts.content.map(prisonerAlertTransformer::transformToApi),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting ACCT alerts for a CRN without a NOMS number returns 404`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
