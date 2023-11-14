package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`

class PersonOffencesTest : IntegrationTestBase() {

  @Test
  fun `Getting offences by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/offences")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting offences for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN that does not exist returns 404`() {
    `Given a User` { _, jwt ->
      val crn = "CRN123"
      mockUnsuccessfulGetCall("/probation-cases/$crn/details", HttpStatus.NOT_FOUND.value())

      webTestClient.get()
        .uri("/people/$crn/offences")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting offences for a CRN returns OK with correct body`() {
    `Given a User` { _, jwt ->
      `Given an Offender`(offenderDetailsConfigBlock = {
        withCurrentExclusion(false).withCurrentRestriction(false)
      },) { offenderDetails, _ ->
        val offence = offenderDetails.offences.first()

        webTestClient.get()
          .uri("/people/${offenderDetails.case.crn}/offences")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json("[{\"deliusEventNumber\":\"${offence.eventNumber}\",\"offenceDescription\":\"${offence.description}\",\"offenceId\":\"\",\"convictionId\":0,\"offenceDate\":\"${offence.date}\"}]")
      }
    }
  }
}
