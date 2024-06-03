package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonApiMockSuccessfulAdjudicationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer

class PersonAdjudicationsTest : InitialiseDatabasePerClassTestBase() {
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
      authSource = "nomis",
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
      authSource = "delius",
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
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      communityApiMockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      webTestClient.get()
        .uri("/people/$crn/adjudications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting adjudications alerts for a CRN without a NOMS number returns 404`() {
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

  @Test
  fun `Getting adjudications for a CRN returns OK with correct body`() {
    givenAUser { _, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val adjudicationsResponse = AdjudicationsPageFactory()
          .withResults(
            listOf(
              AdjudicationFactory().withAgencyId("AGNCY1").produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .withAgencies(
            listOf(
              AgencyFactory().withAgencyId("AGNCY1").produce(),
              AgencyFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .produce()

        prisonApiMockSuccessfulAdjudicationsCall(offenderDetails.otherIds.nomsNumber!!, adjudicationsResponse)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/adjudications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse)),
          )
      }
    }
  }
}
