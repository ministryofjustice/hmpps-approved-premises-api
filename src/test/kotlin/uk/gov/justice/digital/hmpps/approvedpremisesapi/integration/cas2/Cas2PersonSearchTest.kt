package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 POM User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ProbationOffenderSearchAPI_mockForbiddenOffenderSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ProbationOffenderSearchAPI_mockNotFoundSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ProbationOffenderSearchAPI_mockServerErrorSearchCall

class Cas2PersonSearchTest : IntegrationTestBase() {
  @Nested
  inner class PeopleSearchGet {
    @Test
    fun `Searching by NOMIS ID without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber").exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Searching for a NOMIS ID with a non-Delius or NOMIS JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "other source",
      )

      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Searching for a NOMIS ID without ROLE_POM returns 403`() {
      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = "username",
        authSource = "nomis",
        roles = listOf("ROLE_OTHER"),
      )

      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Searching for a NOMIS ID returns Unauthorised error when it is unauthorized`() {
      `Given a CAS2 POM User` { userEntity, jwt ->
        ProbationOffenderSearchAPI_mockForbiddenOffenderSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns 404 error when it is not found`() {
      `Given a CAS2 POM User` { userEntity, jwt ->
        ProbationOffenderSearchAPI_mockNotFoundSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns server error when there is a server error`() {
      `Given a CAS2 POM User` { userEntity, jwt ->
        ProbationOffenderSearchAPI_mockServerErrorSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is5xxServerError
      }
    }
  }
}
