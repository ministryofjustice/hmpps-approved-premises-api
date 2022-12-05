package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConvictionTransformer

class PersonOffencesTest : IntegrationTestBase() {
  @Autowired
  lateinit var convictionTransformer: ConvictionTransformer

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
      authSource = "nomis"
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
      authSource = "delius"
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
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting offences for a CRN returns OK with correct body`() {
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

    val activeConviction = ConvictionFactory()
      .withConvictionId(12345)
      .withIndex("5")
      .withActive(true)
      .withOffences(
        listOf(
          OffenceFactory()
            .withOffenceId("1")
            .withMainCategoryDescription("Main Category 1")
            .withSubCategoryDescription("Sub Category 1")
            .produce(),
          OffenceFactory()
            .withOffenceId("2")
            .withMainCategoryDescription("Main Category 2")
            .withSubCategoryDescription("Sub Category 2")
            .produce()
        )
      )
      .produce()

    val inactiveConviction = ConvictionFactory()
      .withConvictionId(6789)
      .withIndex("2")
      .withActive(false)
      .withOffences(
        listOf(
          OffenceFactory()
            .withOffenceId("3")
            .withMainCategoryDescription("Main Category 1")
            .withSubCategoryDescription("Sub Category 1")
            .produce(),
          OffenceFactory()
            .withOffenceId("4")
            .withMainCategoryDescription("Main Category 2")
            .withSubCategoryDescription("Sub Category 2")
            .produce()
        )
      )
      .produce()

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/CRN/convictions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(
                listOf(
                  activeConviction,
                  inactiveConviction
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
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(convictionTransformer.transformToApi(activeConviction))
      )
  }
}
