package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person

class PersonSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/person/search?crn=CRN")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Searching for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/person/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Searching for a CRN without ROLE_COMMUNITY returns 403`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    mockServer.expect(
      ExpectedCount.once(),
      requestTo("http://community-api/secure/offenders/crn/CRN")
    )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withStatus(HttpStatus.NOT_FOUND))

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/person/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Searching for a CRN that does not exist returns 404`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    mockServer.expect(
      ExpectedCount.once(),
      requestTo("http://community-api/secure/offenders/crn/CRN")
    )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withStatus(HttpStatus.NOT_FOUND))

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_COMMUNITY")
    )

    webTestClient.get()
      .uri("/person/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Searching for a CRN returns OK with correct body`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    mockServer.expect(
      ExpectedCount.once(),
      requestTo("http://community-api/secure/offenders/crn/CRN")
    )
      .andExpect(method(HttpMethod.GET))
      .andRespond(
        withStatus(HttpStatus.OK)
          .body(
            objectMapper.writeValueAsString(
              OffenderDetailsSummaryFactory()
                .withCrn("CRN")
                .withFirstName("James")
                .withLastName("Someone")
                .produce()
            )
          )
      )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_COMMUNITY")
    )

    webTestClient.get()
      .uri("/person/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          Person(
            crn = "CRN",
            name = "James Someone"
          )
        )
      )
  }
}
