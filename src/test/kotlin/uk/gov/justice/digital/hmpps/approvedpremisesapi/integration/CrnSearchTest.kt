package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.CrnSearch
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.CrnSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

class CrnSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for a CRN without a JWT returns 401`() {
    webTestClient.post()
      .uri("/crn/search")
      .bodyValue(CrnSearch(crn = "CRN"))
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

    webTestClient.post()
      .uri("/crn/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(CrnSearch(crn = "CRN"))
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
      authSource = "delius"
    )

    webTestClient.post()
      .uri("/crn/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(CrnSearch(crn = "CRN"))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Searching for a CRN that the username specified in the JWT is excluded from viewing returns 403`() {
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
                .withCurrentExclusion(true)
                .produce()
            )
          )
      )

    mockServer.expect(
      ExpectedCount.once(),
      requestTo("http://community-api/secure/offenders/crn/CRN/user/username/userAccess")
    )
      .andExpect(method(HttpMethod.GET))
      .andRespond(
        withStatus(HttpStatus.OK)
          .body(
            objectMapper.writeValueAsString(UserOffenderAccess(userRestricted = false, userExcluded = true))
          )
      )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.post()
      .uri("/crn/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(CrnSearch(crn = "CRN"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Searching for a CRN that is restricted where the username specified in the JWT is not a user allowed to view returns 403`() {
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
                .withCurrentRestriction(true)
                .produce()
            )
          )
      )

    mockServer.expect(
      ExpectedCount.once(),
      requestTo("http://community-api/secure/offenders/crn/CRN/user/username/userAccess")
    )
      .andExpect(method(HttpMethod.GET))
      .andRespond(
        withStatus(HttpStatus.OK)
          .body(
            objectMapper.writeValueAsString(UserOffenderAccess(userRestricted = true, userExcluded = false))
          )
      )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.post()
      .uri("/crn/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(CrnSearch(crn = "CRN"))
      .exchange()
      .expectStatus()
      .isForbidden
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
      authSource = "delius"
    )

    webTestClient.post()
      .uri("/crn/search")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(CrnSearch(crn = "CRN"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          CrnSearchResult(
            crn = "CRN",
            name = "James Someone"
          )
        )
      )
  }
}
