package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulConvictionsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConvictionTransformer

class PersonOffencesTest : InitialiseDatabasePerClassTestBase() {
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
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      communityApiMockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

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
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

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
                .produce(),
            ),
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
                .produce(),
            ),
          )
          .produce()

        communityApiMockSuccessfulConvictionsCall(offenderDetails.otherIds.crn, listOf(activeConviction, inactiveConviction))

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/offences")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(convictionTransformer.transformToApi(activeConviction)),
          )
      }
    }
  }
}
