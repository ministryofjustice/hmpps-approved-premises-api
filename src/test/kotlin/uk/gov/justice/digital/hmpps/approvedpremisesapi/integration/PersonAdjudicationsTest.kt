package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer

class PersonAdjudicationsTest : IntegrationTestBase() {
  @Autowired
  lateinit var adjudicationTransformer: AdjudicationTransformer

  @Test
  fun `Getting adjudications by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting adjudications for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN that does not exist returns 404`() {
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
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting adjudications for a CRN returns OK with correct body`() {
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
                  .withNomsNumber("NOMS4321")
                  .withFirstName("James")
                  .withLastName("Someone")
                  .produce()
              )
            )
        )
    )

    val adjudicationsResponse = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY1").produce(),
          AdjudicationFactory().withAgencyId("AGNCY2").produce()
        )
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY1").produce(),
          AgencyFactory().withAgencyId("AGNCY2").produce()
        )
      )
      .produce()

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/api/offenders/NOMS4321/adjudications"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(adjudicationsResponse)
            )
        )
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse))
      )
  }
}
